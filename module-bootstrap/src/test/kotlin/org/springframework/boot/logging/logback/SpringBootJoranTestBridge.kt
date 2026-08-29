package org.springframework.boot.logging.logback

import ch.qos.logback.classic.LoggerContext
import org.springframework.boot.logging.LoggingInitializationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import java.net.URL

/**
 * 테스트 전용 브릿지. Spring Boot의 실제 `<springProfile>`/`<springProperty>` 처리기인
 * [SpringBootJoranConfigurator]는 이 패키지(`org.springframework.boot.logging.logback`)
 * 전용(package-private) 클래스라 외부 패키지(`com.yapp.todakun.logging`)에서 직접
 * 참조할 수 없다. 이 파일 하나만 같은 패키지에 두어 그 접근 제약을 우회하고, 실제 테스트
 * 로직/단언은 요청받은 위치인 `com.yapp.todakun.logging.LogbackSpringConfigTest`에 그대로 둔다.
 *
 * ## 왜 공개 API([org.springframework.boot.logging.LoggingSystem]) 대신 이 방식을 쓰는가
 * `LogbackLoggingSystem.initialize(...)`는 내부적으로 `org.slf4j.LoggerFactory.getILoggerFactory()`가
 * 반환하는 **전역** [LoggerContext]를 `stop()`/`reset()`한 뒤 재구성한다 — 테스트 프로세스(같은 JVM에서
 * 도는 다른 테스트들)의 실제 로깅 상태를 오염시킨다. 이 브릿지는 호출자가 새로 만들어 넘긴
 * [LoggerContext] 인스턴스에만 구성을 적용하므로 전역 상태를 전혀 건드리지 않는다.
 */
internal fun configureSpringBootLogback(
    context: LoggerContext,
    environment: ConfigurableEnvironment,
    resourceUrl: URL,
) {
    // StructuredLogEncoder.start()가 getContext().getObject(Environment::class)로 Environment를
    // 직접 조회한다 — 실제 LogbackLoggingSystem.putInitializationContextObjects()와 동일하게 맞춘다.
    context.putObject(Environment::class.java.name, environment)
    val configurator = SpringBootJoranConfigurator(LoggingInitializationContext(environment))
    configurator.context = context
    configurator.doConfigure(resourceUrl)
}
