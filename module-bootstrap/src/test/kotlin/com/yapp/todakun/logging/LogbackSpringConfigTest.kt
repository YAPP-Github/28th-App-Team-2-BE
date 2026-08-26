package com.yapp.todakun.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.spi.FilterReply
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.MarkerFactory
import org.springframework.ai.util.LoggingMarkers
import org.springframework.boot.logging.logback.StructuredLogEncoder
import org.springframework.boot.logging.logback.configureSpringBootLogback
import org.springframework.mock.env.MockEnvironment

/**
 * `logback-spring.xml`의 프로필별 분기(`<springProfile>`)가 실제로 올바른 appender를 붙이는지
 * 검증하는 회귀 테스트다.
 *
 * ## 왜 필요한가
 * logback Joran은 매칭되지 않는 `<springProfile>` 안의 `<appender>`를 아예 인스턴스화하지 않는다.
 * 즉 dev/prod 분기는 배포 전까지 실행되지 않으므로, 그 안의 오타(클래스 FQCN 오기, `<webhookUrl>`
 * setter 불일치, include 리소스 경로 오타 등)는 로컬 프로필로 앱을 부팅/테스트해도 절대 드러나지
 * 않고 **런타임(실제 dev/prod 배포 시점)에야 터진다.** 배포 크리티컬 경로이므로 프로필별로 실제
 * Joran 파서를 돌려 회귀를 막는다.
 *
 * 또한 구조화 콘솔 include(`structured-console-appender.xml`)와 `logging.structured.format.console`
 * 설정은 한 쌍이다 — 둘 중 한쪽만 바뀌면 Alloy → Loki 로그 파이프라인이 **조용히(예외 없이)** 깨진다.
 * CONSOLE appender의 인코더가 여전히 [StructuredLogEncoder]인지까지 확인해 이 회귀도 막는다.
 *
 * ## 전역 상태 격리
 * Spring Boot 공개 API인 `LoggingSystem`/`LogbackLoggingSystem.initialize(...)`는 내부적으로
 * `org.slf4j.LoggerFactory.getILoggerFactory()`가 반환하는 **전역** [LoggerContext]를
 * `stop()`/`reset()`한 뒤 재구성하므로, 테스트에 쓰면 같은 JVM에서 도는 다른 테스트의 실제 로깅
 * 상태를 오염시킨다. 이 클래스는 실제 구성 처리기인 `SpringBootJoranConfigurator`(spring-boot
 * 패키지 전용, package-private)를 [configureSpringBootLogback] 브릿지로 감싸, 테스트마다 새로 만든
 * [LoggerContext] 인스턴스에만 구성을 적용한다 — 전역 LoggerContext는 전혀 건드리지 않는다.
 * 시스템 프로퍼티도 세팅하지 않는다(구조화 포맷은 이 새 컨텍스트에만 지역 스코프로 `putProperty`).
 */
class LogbackSpringConfigTest : DescribeSpec({

    val resourceUrl =
        requireNotNull(Thread.currentThread().contextClassLoader.getResource("logback-spring.xml")) {
            "classpath에서 logback-spring.xml을 찾을 수 없습니다"
        }

    // 프로필별로 새 LoggerContext에 실제 logback-spring.xml을 구성한다. 매 호출마다 완전히
    // 새로운 컨텍스트를 만들어 테스트 간 상태 공유가 없게 한다(전역 오염 없음).
    fun configuredContext(vararg activeProfiles: String): LoggerContext {
        val environment =
            MockEnvironment().apply {
                setActiveProfiles(*activeProfiles)
                // 구조화 콘솔 인코더가 요구하는 포맷 — 실제 dev/prod 프로필 값(logstash)과 동일하게 맞춘다.
                setProperty("logging.structured.format.console", "logstash")
            }
        val context = LoggerContext()
        // Spring Boot 실제 부팅 경로(LogbackLoggingSystemProperties)는 이 값을 시스템 프로퍼티로
        // 옮겨 defaults.xml의 ${CONSOLE_LOG_STRUCTURED_FORMAT:-}가 읽게 한다. 시스템 프로퍼티를
        // 건드리면 전역을 오염시키므로, 이 테스트 전용 컨텍스트에만 지역 스코프로 동일한 값을 심는다.
        context.putProperty("CONSOLE_LOG_STRUCTURED_FORMAT", "logstash")
        configureSpringBootLogback(context, environment, resourceUrl)
        context.start()
        return context
    }

    fun appendersOf(context: LoggerContext): List<Appender<ILoggingEvent>> {
        val root = context.getLogger(ROOT_LOGGER_NAME)
        val appenders = mutableListOf<Appender<ILoggingEvent>>()
        val iterator = root.iteratorForAppenders()
        while (iterator.hasNext()) appenders += iterator.next()
        return appenders
    }

    // 테스트 컨텍스트 정리. **[LoggerContext.stop]을 쓰면 안 된다** — 붙어 있는 모든 appender를
    // stop하는데, logback `OutputStreamAppender.stop()`은 `closeOutputStream()`으로 출력 스트림을
    // 실제로 **닫고**, [ConsoleAppender]의 스트림은 다름 아닌 `System.out`이다. Gradle 테스트
    // 워커는 그 `System.out`으로 데몬과 통신하므로, 닫는 순간 테스트가 전부 통과한 뒤에
    // 워커가 죽어 `java.io.EOFException`으로 빌드 전체가 실패한다(실제로 겪은 증상).
    // CONSOLE은 스레드나 파일 핸들을 보유하지 않으므로 분리만 하고, 나머지 appender만 stop한다.
    fun disposeContext(context: LoggerContext) {
        val root = context.getLogger(ROOT_LOGGER_NAME)
        appendersOf(context).forEach { appender ->
            root.detachAppender(appender)
            if (appender !is ConsoleAppender<*>) appender.stop()
        }
    }

    describe("dev 프로필") {
        it("root에 CONSOLE과 DISCORD가 붙고 DISCORD는 DiscordWebhookAppender + ERROR ThresholdFilter를 가진다") {
            val context = configuredContext("dev")
            try {
                val appenders = appendersOf(context)
                appenders shouldHaveSize 2
                appenders.map { it.name }.toSet() shouldBe setOf("CONSOLE", "DISCORD")

                val discord = appenders.first { it.name == "DISCORD" }.shouldBeInstanceOf<DiscordWebhookAppender>()
                val thresholdFilter = discord.copyOfAttachedFiltersList.filterIsInstance<ThresholdFilter>().singleOrNull()
                requireNotNull(thresholdFilter) { "DISCORD appender에 ThresholdFilter가 없습니다" }
                // ThresholdFilter는 level 필드에 public getter가 없으므로 실제 필터링 동작으로 레벨을 검증한다.
                val testLogger = context.getLogger("com.yapp.todakun.LogbackSpringConfigTest")
                thresholdFilter.decide(LoggingEvent(Logger::class.java.name, testLogger, Level.WARN, "warn", null, null)) shouldBe
                    FilterReply.DENY
                thresholdFilter.decide(LoggingEvent(Logger::class.java.name, testLogger, Level.ERROR, "error", null, null)) shouldBe
                    FilterReply.NEUTRAL
            } finally {
                disposeContext(context)
            }
        }

        it("DISCORD는 SENSITIVE 마커가 붙은 ERROR만 걸러내고 그 외 ERROR는 그대로 통과시킨다") {
            val context = configuredContext("dev")
            try {
                val discord =
                    appendersOf(context).first { it.name == "DISCORD" }.shouldBeInstanceOf<DiscordWebhookAppender>()
                val markerFilter = discord.copyOfAttachedFiltersList.filterIsInstance<SensitiveMarkerFilter>().singleOrNull()
                requireNotNull(markerFilter) { "DISCORD appender에 SensitiveMarkerFilter가 없습니다" }

                val testLogger = context.getLogger("com.yapp.todakun.LogbackSpringConfigTest")
                val plainError = LoggingEvent(Logger::class.java.name, testLogger, Level.ERROR, "error", null, null)
                val sensitiveError =
                    LoggingEvent(Logger::class.java.name, testLogger, Level.ERROR, "error", null, null).apply {
                        addMarker(LoggingMarkers.SENSITIVE_DATA_MARKER)
                    }
                markerFilter.decide(plainError) shouldBe FilterReply.NEUTRAL
                markerFilter.decide(sensitiveError) shouldBe FilterReply.DENY
            } finally {
                disposeContext(context)
            }
        }

        it("SENSITIVE 마커를 참조로 포함한 계층형(composite) 마커도 걸러낸다") {
            val context = configuredContext("dev")
            try {
                val discord =
                    appendersOf(context).first { it.name == "DISCORD" }.shouldBeInstanceOf<DiscordWebhookAppender>()
                val markerFilter = discord.copyOfAttachedFiltersList.filterIsInstance<SensitiveMarkerFilter>().singleOrNull()
                requireNotNull(markerFilter) { "DISCORD appender에 SensitiveMarkerFilter가 없습니다" }

                val testLogger = context.getLogger("com.yapp.todakun.LogbackSpringConfigTest")
                val compositeMarker =
                    MarkerFactory.getMarker("COMPOSITE_SENSITIVE").apply {
                        add(LoggingMarkers.SENSITIVE_DATA_MARKER)
                    }
                val compositeSensitiveError =
                    LoggingEvent(Logger::class.java.name, testLogger, Level.ERROR, "error", null, null).apply {
                        addMarker(compositeMarker)
                    }
                markerFilter.decide(compositeSensitiveError) shouldBe FilterReply.DENY
            } finally {
                disposeContext(context)
            }
        }

        it("CONSOLE appender의 인코더가 StructuredLogEncoder다(구조화 콘솔 → Alloy → Loki 파이프라인 유지)") {
            val context = configuredContext("dev")
            try {
                val console =
                    appendersOf(context).first { it.name == "CONSOLE" }.shouldBeInstanceOf<ConsoleAppender<ILoggingEvent>>()
                console.encoder.shouldBeInstanceOf<StructuredLogEncoder>()
            } finally {
                disposeContext(context)
            }
        }
    }

    describe("prod 프로필") {
        // prod는 장애 영향이 가장 큰 환경이므로 dev와 같은 깊이로 검증한다.
        it("dev와 동일하게 CONSOLE과 DISCORD가 붙고 구조화 인코더도 유지된다") {
            val context = configuredContext("prod")
            try {
                val appenders = appendersOf(context)
                appenders shouldHaveSize 2
                appenders.map { it.name }.toSet() shouldBe setOf("CONSOLE", "DISCORD")

                val discord = appenders.first { it.name == "DISCORD" }.shouldBeInstanceOf<DiscordWebhookAppender>()
                val thresholdFilter = discord.copyOfAttachedFiltersList.filterIsInstance<ThresholdFilter>().singleOrNull()
                requireNotNull(thresholdFilter) { "DISCORD appender에 ThresholdFilter가 없습니다" }
                val testLogger = context.getLogger("com.yapp.todakun.LogbackSpringConfigTest")
                thresholdFilter.decide(LoggingEvent(Logger::class.java.name, testLogger, Level.WARN, "warn", null, null)) shouldBe
                    FilterReply.DENY
                thresholdFilter.decide(LoggingEvent(Logger::class.java.name, testLogger, Level.ERROR, "error", null, null)) shouldBe
                    FilterReply.NEUTRAL

                val markerFilter = discord.copyOfAttachedFiltersList.filterIsInstance<SensitiveMarkerFilter>().singleOrNull()
                requireNotNull(markerFilter) { "DISCORD appender에 SensitiveMarkerFilter가 없습니다" }
                val sensitiveError =
                    LoggingEvent(Logger::class.java.name, testLogger, Level.ERROR, "error", null, null).apply {
                        addMarker(LoggingMarkers.SENSITIVE_DATA_MARKER)
                    }
                markerFilter.decide(LoggingEvent(Logger::class.java.name, testLogger, Level.ERROR, "error", null, null)) shouldBe
                    FilterReply.NEUTRAL
                markerFilter.decide(sensitiveError) shouldBe FilterReply.DENY

                val console =
                    appenders.first { it.name == "CONSOLE" }.shouldBeInstanceOf<ConsoleAppender<ILoggingEvent>>()
                console.encoder.shouldBeInstanceOf<StructuredLogEncoder>()
            } finally {
                disposeContext(context)
            }
        }
    }

    describe("local(그 외) 프로필") {
        it("CONSOLE만 붙고 DISCORD는 없다") {
            val context = configuredContext("local")
            try {
                val appenders = appendersOf(context)
                appenders.map { it.name }.toSet() shouldBe setOf("CONSOLE")
            } finally {
                disposeContext(context)
            }
        }
    }
})
