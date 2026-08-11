package com.yapp.todakun.config

import com.yapp.todakun.common.resilience.AiResilienceSupport
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics
import io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetrics
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val AI_CALL_EXECUTOR_POOL_SIZE = 20

/**
 * [AiResilienceProperties]를 읽어 CircuitBreaker/Retry/TimeLimiter 레지스트리를 구성하고,
 * 메트릭을 기존 [MeterRegistry](Prometheus로 이미 노출 중)에 바인딩한다.
 * CircuitBreaker는 인스턴스 이름과 무관하게 레지스트리 공통 기본값 하나를 쓰므로, 이름별로 미리 등록해 둘 필요 없이
 * [AiResilienceSupport]가 처음 그 이름을 쓸 때 레지스트리가 자동으로 만들어 준다.
 * `retries`/`timeLimiters`에 이름이 없는 인스턴스는 해당 레지스트리에 그 이름을 등록하지 않는다.
 * [AiResilienceSupport]가 이를 "해당 단계 비적용"으로 해석한다.
 */
@Configuration
@EnableConfigurationProperties(AiResilienceProperties::class)
class AiResilienceConfig {
    @Bean(destroyMethod = "shutdown")
    fun aiCallExecutor(): ExecutorService = Executors.newFixedThreadPool(AI_CALL_EXECUTOR_POOL_SIZE)

    @Bean
    fun aiCircuitBreakerRegistry(
        properties: AiResilienceProperties,
        meterRegistry: MeterRegistry,
    ): CircuitBreakerRegistry {
        val registry = CircuitBreakerRegistry.of(properties.circuitBreaker.toConfig())
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry)

        return registry
    }

    @Bean
    fun aiRetryRegistry(
        properties: AiResilienceProperties,
        meterRegistry: MeterRegistry,
    ): RetryRegistry {
        val registry = RetryRegistry.ofDefaults()
        properties.retries.forEach { (name, settings) -> registry.retry(name, settings.toConfig()) }
        TaggedRetryMetrics.ofRetryRegistry(registry).bindTo(meterRegistry)

        return registry
    }

    @Bean
    fun aiTimeLimiterRegistry(
        properties: AiResilienceProperties,
        meterRegistry: MeterRegistry,
    ): TimeLimiterRegistry {
        val registry = TimeLimiterRegistry.ofDefaults()
        properties.timeLimiters.forEach { (name, settings) -> registry.timeLimiter(name, settings.toConfig()) }
        TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(registry).bindTo(meterRegistry)

        return registry
    }

    @Bean
    fun aiResilienceSupport(
        circuitBreakerRegistry: CircuitBreakerRegistry,
        retryRegistry: RetryRegistry,
        timeLimiterRegistry: TimeLimiterRegistry,
        aiCallExecutor: ExecutorService,
    ): AiResilienceSupport = AiResilienceSupport(circuitBreakerRegistry, retryRegistry, timeLimiterRegistry, aiCallExecutor)
}
