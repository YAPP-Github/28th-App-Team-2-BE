package com.yapp.todakun.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Discord content 필드의 실제 상한. appender는 이보다 낮은 값으로 절단한다. */
private const val DISCORD_CONTENT_HARD_LIMIT = 2000

/**
 * [DiscordWebhookAppender] 순수 단위 테스트. 스프링 컨텍스트를 띄우지 않고, 실제 Discord
 * 서버 대신 JDK 내장 [HttpServer]로 로컬 포트 0에 스텁 서버를 띄워 검증한다(새 의존성 추가 없음).
 */
class DiscordWebhookAppenderTest : DescribeSpec({

    fun newAppender(): DiscordWebhookAppender =
        DiscordWebhookAppender().apply {
            context = LoggerContext()
        }

    fun errorEvent(
        message: String,
        throwable: Throwable? = null,
    ): LoggingEvent {
        val loggerContext = LoggerContext()
        val logger = loggerContext.getLogger("com.yapp.todakun.DiscordWebhookAppenderTest") as Logger
        return LoggingEvent(Logger::class.java.name, logger, Level.ERROR, message, throwable, null)
    }

    // appender 인스턴스가 실제로 띄운 스레드만 본다. 이름이 인스턴스마다 고유하므로
    // 다른 테스트가 남긴 워커를 자기 것으로 오인하지 않는다(순서·병렬 실행에 영향받지 않음).
    fun workerThreadAlive(appender: DiscordWebhookAppender): Boolean =
        Thread.getAllStackTraces().keys.any { it.name == appender.workerThreadName && it.isAlive }

    /** 스텁 웹훅 서버. 상태 코드·지연을 파라미터로 받아, 테스트마다 서버를 새로 짜지 않는다. */
    fun startStubServer(
        statusCode: Int = 204,
        delayMs: Long = 0L,
        onRequest: (String) -> Unit = {},
    ): HttpServer {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/webhook") { exchange ->
            val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            onRequest(body)
            if (delayMs > 0) Thread.sleep(delayMs)
            exchange.sendResponseHeaders(statusCode, -1)
            exchange.close()
        }
        server.start()
        return server
    }

    describe("webhookUrl 미설정") {
        it("워커를 띄우지 않고 완전히 no-op으로 동작한다") {
            val requestCount = AtomicInteger(0)
            val server = startStubServer { requestCount.incrementAndGet() }

            try {
                val appender = newAppender()
                // webhookUrl을 설정하지 않는다(기본값 null) — 완전 no-op이어야 한다.

                shouldNotThrowAny {
                    appender.start()
                    appender.doAppend(errorEvent("설정 안 된 상태에서의 에러"))
                    appender.doAppend(errorEvent("두 번째 에러"))
                }

                Thread.sleep(300)
                requestCount.get() shouldBe 0
                workerThreadAlive(appender) shouldBe false

                appender.stop()
            } finally {
                server.stop(0)
            }
        }

        it("빈 문자열/공백 webhookUrl도 no-op으로 동작한다") {
            val appender = newAppender()
            appender.webhookUrl = "   "

            shouldNotThrowAny {
                appender.start()
                appender.doAppend(errorEvent("공백 webhookUrl 케이스"))
            }
            workerThreadAlive(appender) shouldBe false

            appender.stop()
        }
    }

    describe("ERROR 이벤트 전송") {
        it("웹훅으로 POST 요청을 보내고 본문에 메시지와 스택 트레이스가 포함된다") {
            val latch = CountDownLatch(1)
            val receivedBody = AtomicReference<String>()
            val server =
                startStubServer { body ->
                    receivedBody.set(body)
                    latch.countDown()
                }

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.start()

                appender.doAppend(errorEvent("결제 처리 중 오류 발생", IllegalStateException("잔액 부족")))

                latch.await(5, TimeUnit.SECONDS) shouldBe true
                val body = receivedBody.get()
                body shouldContain "결제 처리 중 오류 발생"
                body shouldContain "IllegalStateException"
                body shouldContain "\"content\""

                appender.stop()
            } finally {
                server.stop(0)
            }
        }
    }

    describe("전송 실패") {
        it("연결 불가한 웹훅이어도 append()는 예외를 던지지 않고 워커도 죽지 않는다") {
            // 포트를 확보한 뒤 바로 닫아 아무도 리스닝하지 않는 상태를 만든다(connection refused 유도).
            val closedPort = ServerSocket(0).use { it.localPort }
            val appender = newAppender()
            appender.webhookUrl = "http://localhost:$closedPort/webhook"
            appender.start()

            shouldNotThrowAny {
                appender.doAppend(errorEvent("연결 불가 상황", RuntimeException("boom")))
            }

            // 워커가 백그라운드에서 전송 실패를 처리할 시간을 준다 — 예외로 죽지 않고 살아있어야 한다.
            Thread.sleep(300)
            workerThreadAlive(appender) shouldBe true

            appender.stop()
        }

        it("500 응답을 반환해도 append()는 예외를 던지지 않는다") {
            // 요청이 실제로 도달했는지까지 확인한다 — shouldNotThrowAny만 두면
            // 워커가 아예 전송하지 않아도 테스트가 통과해 버린다.
            val latch = CountDownLatch(1)
            val server = startStubServer(statusCode = 500) { latch.countDown() }

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.start()

                shouldNotThrowAny {
                    appender.doAppend(errorEvent("500 응답 케이스"))
                }

                latch.await(5, TimeUnit.SECONDS) shouldBe true
                // 비2xx여도 워커는 살아남아 다음 이벤트를 계속 처리해야 한다.
                workerThreadAlive(appender) shouldBe true

                appender.stop()
            } finally {
                server.stop(0)
            }
        }
    }

    describe("Discord 길이 상한(2000자) 절단") {
        it("스택 트레이스가 잘려도 코드 블록이 닫힌 채로 전송된다") {
            val latch = CountDownLatch(1)
            val receivedBody = AtomicReference<String>()
            val server =
                startStubServer { body ->
                    receivedBody.set(body)
                    latch.countDown()
                }

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.start()

                // 상한을 확실히 넘기도록 깊은 원인 체인을 만든다.
                var deep: Throwable = IllegalStateException("가장 깊은 원인")
                repeat(50) { i -> deep = RuntimeException("래핑 $i", deep) }
                appender.doAppend(errorEvent("아주 긴 스택 트레이스", deep))

                latch.await(5, TimeUnit.SECONDS) shouldBe true

                // 전송된 JSON에서 content 값을 꺼내 실제 Discord가 받는 문자열로 검증한다.
                val content = ObjectMapper().readTree(receivedBody.get()).get("content").asString()
                content.length shouldBeLessThanOrEqual DISCORD_CONTENT_HARD_LIMIT
                content shouldContain "... (truncated)"
                // 여는 펜스와 닫는 펜스 개수가 같아야 렌더링이 깨지지 않는다.
                content.windowed(3).count { it == "```" } % 2 shouldBe 0
                content.trimEnd().endsWith("```") shouldBe true

                appender.stop()
            } finally {
                server.stop(0)
            }
        }

        it("스택 트레이스가 없는 짧은 메시지에는 코드 블록 표시가 붙지 않는다") {
            val latch = CountDownLatch(1)
            val receivedBody = AtomicReference<String>()
            val server =
                startStubServer { body ->
                    receivedBody.set(body)
                    latch.countDown()
                }

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.start()

                appender.doAppend(errorEvent("스택 트레이스 없는 에러"))

                latch.await(5, TimeUnit.SECONDS) shouldBe true
                val content = ObjectMapper().readTree(receivedBody.get()).get("content").asString()
                content shouldContain "스택 트레이스 없는 에러"
                content.contains("```") shouldBe false

                appender.stop()
            } finally {
                server.stop(0)
            }
        }
    }

    describe("종료 시 큐 드레인") {
        it("stop() 시점에 큐에 쌓여 있던 알림을 모두 전송한 뒤 종료한다") {
            // 종료 직전 ERROR(SIGTERM 직전 예외·OOM 등)가 가장 중요한 알림이므로 유실되면 안 된다.
            val backlog = 5
            val received = AtomicInteger(0)
            // 워커를 느리게 만들어 stop() 호출 시점에 큐에 backlog가 남아 있게 한다.
            val server = startStubServer(delayMs = 50) { received.incrementAndGet() }

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.start()

                repeat(backlog) { i -> appender.doAppend(errorEvent("종료 직전 에러 $i")) }
                appender.stop()

                // stop()이 반환한 시점엔 드레인이 끝나 있어야 한다(추가 대기 없이 단언).
                received.get() shouldBe backlog
            } finally {
                server.stop(0)
            }
        }

        it("드레인 제한시간을 넘기면 남은 알림을 버리고 종료한다(무한 대기 금지)") {
            // 제한시간 안에 다 비울 수 없을 만큼 느리게 응답한다.
            val server = startStubServer(delayMs = 400)

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.drainTimeoutMs = 200
                appender.start()

                repeat(20) { i -> appender.doAppend(errorEvent("느린 전송 $i")) }

                val startedAt = System.nanoTime()
                appender.stop()
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

                // 드레인(200ms) + join 상한 안에서 끝나야 한다 — graceful shutdown을 막으면 안 된다.
                elapsedMs shouldBeLessThan 6_000L
                appender.isStarted shouldBe false
            } finally {
                server.stop(0)
            }
        }
    }

    describe("stop()") {
        it("워커 스레드를 정리하고 isStarted를 false로 만든다") {
            val server = startStubServer()

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.start()

                workerThreadAlive(appender) shouldBe true

                appender.stop()

                appender.isStarted shouldBe false

                val deadlineMs = System.currentTimeMillis() + 2000
                var stillAlive = workerThreadAlive(appender)
                while (stillAlive && System.currentTimeMillis() < deadlineMs) {
                    Thread.sleep(100)
                    stillAlive = workerThreadAlive(appender)
                }
                stillAlive shouldBe false
            } finally {
                server.stop(0)
            }
        }
    }

    describe("큐 포화") {
        it("queueSize를 초과해도 append()는 블로킹하지 않는다") {
            // 워커가 계속 바쁘도록 응답을 지연시켜 큐가 비워지지 않게 한다.
            val server = startStubServer(delayMs = 200)

            try {
                val appender = newAppender()
                appender.webhookUrl = "http://localhost:${server.address.port}/webhook"
                appender.queueSize = 1
                appender.start()

                val startedAt = System.nanoTime()
                repeat(50) { i -> appender.doAppend(errorEvent("큐 포화 테스트 $i")) }
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

                elapsedMs shouldBeLessThan 1000L

                appender.stop()
            } finally {
                server.stop(0)
            }
        }
    }
})
