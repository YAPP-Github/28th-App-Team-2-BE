package com.yapp.todakun.config

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 도메인별 AI 호출 Resilience4j 설정(`ai-resilience.*`).
 * `circuitBreaker`는 현재 5개 인스턴스가 모두 동일한 값을 쓰므로 인스턴스별이 아닌 레지스트리 공통 기본값 하나만 둔다
 * (도메인별로 다르게 튜닝할 실사용 요구가 생기면 그때 인스턴스별 설정으로 확장한다).
 * `retries`/`timeLimiters`는 인스턴스 이름(`chat-ai` 등)이 키에 없으면 [AiResilienceConfig]가 해당 레지스트리에 그
 * 이름을 등록하지 않아 [com.yapp.todakun.common.resilience.AiResilienceSupport.execute]가 그 단계를 건너뛴다.
 */
@ConfigurationProperties(prefix = "ai-resilience")
data class AiResilienceProperties(
    val circuitBreaker: CircuitBreakerSettings = CircuitBreakerSettings(),
    val executor: ExecutorSettings = ExecutorSettings(),
    val retries: Map<String, RetrySettings> = emptyMap(),
    val timeLimiters: Map<String, TimeLimiterSettings> = emptyMap(),
) {
    data class CircuitBreakerSettings(
        val slidingWindowSize: Int = 20,
        val minimumNumberOfCalls: Int = 10,
        val failureRateThreshold: Float = 50f,
        val waitDurationInOpenStateSeconds: Long = 30,
        val permittedNumberOfCallsInHalfOpenState: Int = 5,
    ) {
        fun toConfig(): CircuitBreakerConfig =
            CircuitBreakerConfig
                .custom()
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenStateSeconds))
                .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                .build()
    }

    data class RetrySettings(
        val maxAttempts: Int = 3,
        val waitDurationMillis: Long = 500,
    ) {
        fun toConfig(): RetryConfig =
            RetryConfig
                .custom<Any>()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(waitDurationMillis))
                // 회로 OPEN 차단은 즉시 실패시킨다(재시도 대기로 fail-fast가 무의미해지는 것을 방지).
                .ignoreExceptions(CallNotPermittedException::class.java)
                .build()
    }

    data class TimeLimiterSettings(
        val timeoutDurationSeconds: Long = 15,
    ) {
        fun toConfig(): TimeLimiterConfig = TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(timeoutDurationSeconds)).build()
    }

    /**
     * TimeLimiter를 쓰는 각 인스턴스([com.yapp.todakun.common.resilience.AiResilienceSupport]가 인스턴스별로 지연 생성하는 전용 executor)에 공통으로 적용하는 크기값.
     * 도메인마다 다른 값을 쓸 실사용 요구가 없어 [circuitBreaker]와 같은 방식으로 레지스트리 공통 기본값 하나만 둔다.
     * 격리는 인스턴스별로 별개의 풀 "객체"를 만드는 데서 오지, 크기를 다르게 주는 데서 오지 않는다.
     * 큐가 가득 차면 [java.util.concurrent.ThreadPoolExecutor.AbortPolicy]로 즉시 거절해 fail-fast한다(무제한 큐에 쌓여 지연이 계속 늘어나는 것을 방지).
     */
    data class ExecutorSettings(
        val poolSize: Int = 4,
        val queueCapacity: Int = 10,
    )
}
