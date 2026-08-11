package com.yapp.todakun.config

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
                .build()
    }

    data class TimeLimiterSettings(
        val timeoutDurationSeconds: Long = 15,
    ) {
        fun toConfig(): TimeLimiterConfig = TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(timeoutDurationSeconds)).build()
    }
}
