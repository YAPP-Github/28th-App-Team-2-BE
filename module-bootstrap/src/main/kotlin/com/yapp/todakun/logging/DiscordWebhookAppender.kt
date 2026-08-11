package com.yapp.todakun.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.UnsynchronizedAppenderBase
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ERROR 로그를 Discord 웹훅으로 직접 발송하는 logback appender.
 *
 * ## 왜 직접 구현했는가 (외부 라이브러리 `com.github.napstr:logback-discord-appender` 미채택 사유)
 * - Maven Central에 배포되지 않고 JitPack에만 있어 빌드 의존성으로 신뢰하기 어렵다.
 * - non-daemon 스레드를 쓰고 `stop()`을 구현하지 않아, Blue/Green 배포의 SIGTERM graceful
 *   shutdown을 워커 스레드가 붙잡아 지연/방해할 수 있다.
 * - webhookUrl이 비어 있을 때도 큐에 무한히 쌓이며(백프레셔 없음) 재전송을 시도해 메모리를 위협한다.
 * - 관리가 뜸해 전이 의존성(OkHttp 등)이 낡았다.
 *
 * ## 설계 원칙 (로깅 경로가 애플리케이션을 방해해선 안 된다)
 * - `webhookUrl` 미설정(null/blank) 시 워커 스레드조차 띄우지 않는 완전 no-op.
 * - 유한 큐([ArrayBlockingQueue]) + `offer()`(non-blocking) — 큐가 가득 차면 이벤트를 버린다.
 * - 데몬 워커 스레드 1개 — JVM graceful shutdown을 막지 않는다.
 * - `append()`/워커 루프 전체를 예외로부터 격리 — 전송 실패가 애플리케이션에 전파되지 않는다.
 * - 내부 실패는 slf4j가 아니라 logback 자체 `addWarn`/`addError`로만 남긴다(로깅 재귀 방지).
 *
 * HTTP 전송은 JDK 내장 [HttpClient]를 쓴다(새 의존성 추가 없음). JSON 직렬화는 Spring 빈에
 * 의존하지 않도록 [ObjectMapper]를 이 클래스가 직접 생성해 보관한다.
 */
class DiscordWebhookAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    /** logback XML의 `<webhookUrl>` 태그로 주입되는 프로퍼티. null/blank면 완전 비활성. */
    var webhookUrl: String? = null

    /** 전송 대기 큐 용량. 가득 차면 새 이벤트를 버린다(블로킹 금지). */
    var queueSize: Int = DEFAULT_QUEUE_SIZE

    /** Discord로의 HTTP 연결 타임아웃(ms). */
    var connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS

    /** Discord로의 HTTP 요청(응답 포함) 타임아웃(ms). */
    var requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS

    private val objectMapper = ObjectMapper()

    @Volatile
    private var enabled = false

    @Volatile
    private var running = false

    private val queueFullWarned = AtomicBoolean(false)

    private lateinit var queue: ArrayBlockingQueue<String>
    private lateinit var httpClient: HttpClient
    private lateinit var resolvedWebhookUrl: String
    private var worker: Thread? = null

    override fun start() {
        val url = webhookUrl
        if (url.isNullOrBlank()) {
            addWarn("DISCORD_WEBHOOK_URL 미설정 — Discord 알림 비활성")
            enabled = false
            super.start()
            return
        }

        enabled = true
        resolvedWebhookUrl = url
        queue = ArrayBlockingQueue(queueSize)
        httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build()

        running = true
        worker =
            Thread(::runWorkerLoop).apply {
                isDaemon = true
                name = WORKER_THREAD_NAME
            }
        worker?.start()

        super.start()
    }

    override fun stop() {
        if (!isStarted) return

        running = false
        worker?.interrupt()
        try {
            worker?.join(WORKER_JOIN_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        super.stop()
    }

    override fun append(eventObject: ILoggingEvent) {
        if (!enabled) return

        try {
            val payload = buildPayloadJson(eventObject)
            if (!queue.offer(payload) && queueFullWarned.compareAndSet(false, true)) {
                addWarn("Discord 웹훅 큐가 가득 차 로그 이벤트를 드롭합니다(queueSize=$queueSize)")
            }
        } catch (t: Throwable) {
            addError("Discord 웹훅 페이로드 생성 실패", t)
        }
    }

    private fun runWorkerLoop() {
        while (running) {
            val payload =
                try {
                    queue.poll(WORKER_POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    null
                }
            if (payload == null) continue

            try {
                sendToDiscord(payload)
            } catch (t: Throwable) {
                addError("Discord 웹훅 전송 실패", t)
            }
        }
    }

    private fun sendToDiscord(payloadJson: String) {
        try {
            val request =
                HttpRequest
                    .newBuilder(URI.create(resolvedWebhookUrl))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson, Charsets.UTF_8))
                    .build()
            httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        } catch (t: Throwable) {
            addError("Discord 웹훅 HTTP 요청 실패", t)
        }
    }

    private fun buildPayloadJson(event: ILoggingEvent): String {
        val stackTrace = event.throwableProxy?.let { ThrowableProxyUtil.asString(it) }
        val header = "**[${event.level}]** `${event.loggerName}` (thread: ${event.threadName})\n${event.formattedMessage}"
        val content = if (stackTrace != null) "$header\n```\n$stackTrace\n```" else header
        return objectMapper.writeValueAsString(mapOf("content" to truncate(content)))
    }

    private fun truncate(content: String): String {
        if (content.length <= DISCORD_CONTENT_MAX_LENGTH) return content
        val cutoff = DISCORD_CONTENT_MAX_LENGTH - TRUNCATION_SUFFIX.length
        return content.take(cutoff) + TRUNCATION_SUFFIX
    }

    companion object {
        private const val DEFAULT_QUEUE_SIZE = 256
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 3000L
        private const val DEFAULT_REQUEST_TIMEOUT_MS = 5000L
        private const val WORKER_THREAD_NAME = "discord-webhook-appender"
        private const val WORKER_JOIN_TIMEOUT_MS = 2000L
        private const val WORKER_POLL_TIMEOUT_SECONDS = 1L

        // Discord 웹훅 content 필드 상한은 2000자. JSON 이스케이프/래핑 오버헤드를 감안해 여유를 둔다.
        private const val DISCORD_CONTENT_MAX_LENGTH = 1900
        private const val TRUNCATION_SUFFIX = "\n... (truncated)"
    }
}
