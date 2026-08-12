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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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

    /** 종료 시 큐에 남은 알림을 보내려고 기다리는 최대 시간(ms). graceful shutdown 상한을 위해 유한하다. */
    var drainTimeoutMs: Long = DEFAULT_DRAIN_TIMEOUT_MS

    private val objectMapper = ObjectMapper()

    @Volatile
    private var enabled = false

    @Volatile
    private var running = false

    /** 누적 드롭 수. 일회성 플래그로 두면 첫 드롭 이후의 지속적인 유실이 로그에 남지 않는다. */
    private val droppedCount = AtomicLong(0)

    /**
     * 워커 스레드 이름. 인스턴스마다 고유하다 — 이름이 같으면 여러 appender(특히 테스트)가
     * 서로의 워커 스레드를 자기 것으로 오인한다.
     */
    internal val workerThreadName: String = "$WORKER_THREAD_NAME_PREFIX-${INSTANCE_COUNTER.incrementAndGet()}"

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
                name = workerThreadName
            }
        worker?.start()

        super.start()
    }

    /**
     * 종료 순서: ①새 이벤트 수락 중지 → ②큐 드레인(제한시간까지) → ③워커 자연 종료 대기 →
     * ④그래도 살아 있으면 인터럽트.
     *
     * 종료 직전에 쌓인 ERROR야말로 가장 받아봐야 할 알림이다(SIGTERM 직전 예외, OOM 등).
     * 곧바로 인터럽트하면 그게 통째로 유실된다. 반대로 무한정 기다리면 Blue/Green 배포의
     * graceful shutdown을 막으므로, 전체 대기는 [drainTimeoutMs] + join 2회로 상한이 있다.
     */
    override fun stop() {
        if (!isStarted) return

        val wasEnabled = enabled
        enabled = false // 이 시점 이후의 append()는 즉시 반환한다(큐가 다시 차지 않게)

        if (wasEnabled) {
            drainQueue()
        }

        running = false
        // 인터럽트를 먼저 쏘면 전송 중인 요청까지 끊긴다. 워커는 poll 타임아웃(1초) 후
        // running=false를 보고 스스로 빠져나오므로, 자연 종료를 먼저 기다린다.
        joinWorker()
        if (worker?.isAlive == true) {
            worker?.interrupt()
            joinWorker()
        }
        worker = null

        super.stop()
    }

    /** 큐가 빌 때까지(또는 제한시간까지) 워커가 남은 알림을 보내도록 기다린다. */
    private fun drainQueue() {
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(drainTimeoutMs)
        try {
            while (queue.isNotEmpty() && System.nanoTime() < deadlineNanos) {
                Thread.sleep(DRAIN_POLL_INTERVAL_MS)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        val remaining = queue.size
        if (remaining > 0) {
            addWarn("종료 드레인 제한시간(${drainTimeoutMs}ms) 안에 보내지 못한 Discord 알림 ${remaining}건을 버립니다")
        }
    }

    private fun joinWorker() {
        try {
            worker?.join(WORKER_JOIN_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    override fun append(eventObject: ILoggingEvent) {
        if (!enabled) return

        try {
            val payload = buildPayloadJson(eventObject)
            if (!queue.offer(payload)) {
                // 첫 드롭과 이후 일정 간격마다 누적 수를 남긴다 — 일회성 플래그로 두면
                // 드롭이 계속되는 상황과 한 번 튄 상황을 운영에서 구분할 수 없다.
                val dropped = droppedCount.incrementAndGet()
                if (dropped == 1L || dropped % DROP_WARN_INTERVAL == 0L) {
                    addWarn("Discord 웹훅 큐가 가득 차 로그 이벤트를 드롭합니다(queueSize=$queueSize, 누적 드롭=$dropped)")
                }
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
                    // 인터럽트 플래그만 복원하고 continue하면, running이 true인 채로 외부가
                    // 인터럽트했을 때 poll()이 즉시 예외를 던지는 과정을 반복해 CPU를 태운다.
                    // 인터럽트는 종료 신호로 보고 루프를 빠져나간다.
                    Thread.currentThread().interrupt()
                    return
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
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            // 상태 코드를 버리면 Discord의 429(요청 제한)·40x가 조용히 묻혀,
            // "알림이 안 온다"는 상황의 원인을 운영에서 진단할 수 없다.
            if (response.statusCode() !in 200..299) {
                addWarn("Discord 웹훅 응답이 비정상입니다(status=${response.statusCode()}, body=${response.body().take(RESPONSE_BODY_LOG_LIMIT)})")
            }
        } catch (t: Throwable) {
            addError("Discord 웹훅 HTTP 요청 실패", t)
        }
    }

    private fun buildPayloadJson(event: ILoggingEvent): String {
        val stackTrace = event.throwableProxy?.let { ThrowableProxyUtil.asString(it) }
        val header = "**[${event.level}]** `${event.loggerName}` (thread: ${event.threadName})\n${event.formattedMessage}"
        val content = if (stackTrace != null) "$header\n$CODE_FENCE\n$stackTrace\n$CODE_FENCE" else header
        // 코드 블록으로 감싼 경우 절단 접미사에 닫는 펜스를 포함해야 한다. 스택 트레이스는 대부분
        // 상한을 넘기므로, 그냥 끝을 자르면 여는 ```만 남아 Discord 메시지 렌더링이 깨진다.
        val suffix = if (stackTrace != null) "$TRUNCATION_SUFFIX\n$CODE_FENCE" else TRUNCATION_SUFFIX
        return objectMapper.writeValueAsString(mapOf("content" to truncate(content, suffix)))
    }

    private fun truncate(
        content: String,
        suffix: String,
    ): String {
        if (content.length <= DISCORD_CONTENT_MAX_LENGTH) return content
        return content.take(DISCORD_CONTENT_MAX_LENGTH - suffix.length) + suffix
    }

    companion object {
        private const val DEFAULT_QUEUE_SIZE = 256
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 3000L
        private const val DEFAULT_REQUEST_TIMEOUT_MS = 5000L
        private const val WORKER_THREAD_NAME_PREFIX = "discord-webhook-appender"
        private const val WORKER_JOIN_TIMEOUT_MS = 2000L
        private const val WORKER_POLL_TIMEOUT_SECONDS = 1L
        private const val DEFAULT_DRAIN_TIMEOUT_MS = 2000L
        private const val DRAIN_POLL_INTERVAL_MS = 20L

        /** 워커 스레드 이름을 인스턴스마다 고유하게 만들기 위한 일련번호. */
        private val INSTANCE_COUNTER = AtomicInteger(0)

        /** 드롭 경고를 남기는 간격(누적 드롭 수 기준). 매 드롭마다 남기면 그 자체가 로그 폭주가 된다. */
        private const val DROP_WARN_INTERVAL = 100L

        /** 비정상 응답 진단용으로 남길 응답 본문 길이 상한. */
        private const val RESPONSE_BODY_LOG_LIMIT = 200

        private const val CODE_FENCE = "```"

        // Discord 웹훅 content 필드 상한은 2000자. JSON 이스케이프/래핑 오버헤드를 감안해 여유를 둔다.
        private const val DISCORD_CONTENT_MAX_LENGTH = 1900
        private const val TRUNCATION_SUFFIX = "\n... (truncated)"
    }
}
