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
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * [AiResilienceProperties]를 읽어 CircuitBreaker/Retry/TimeLimiter 레지스트리를 구성하고,
 * 메트릭을 기존 [MeterRegistry](Prometheus로 이미 노출 중)에 바인딩한다.
 * CircuitBreaker는 인스턴스 이름과 무관하게 레지스트리 공통 기본값 하나를 쓰므로, 이름별로 미리 등록해 둘 필요 없이
 * [AiResilienceSupport]가 처음 그 이름을 쓸 때 레지스트리가 자동으로 만들어 준다.
 * `retries`/`timeLimiters`에 이름이 없는 인스턴스는 해당 레지스트리에 그 이름을 등록하지 않는다.
 * [AiResilienceSupport]가 이를 "해당 단계 비적용"으로 해석한다.
 *
 * executor는 모든 도메인이 공유하는 풀 하나 대신,
 * [AiResilienceSupport]가 인스턴스 이름별로 이 팩토리를 호출해 전용 [ThreadPoolExecutor]를 지연 생성하게 한다.
 * Vertex AI 호출이 timeout에도 스레드를 계속 점유하는 도메인이 있어도 그 도메인의 풀만 포화되고 다른 도메인 요청은 영향받지 않는다.
 * 큐까지 가득 차면 [ThreadPoolExecutor.AbortPolicy]로 즉시 거절해(무제한 큐 적체 방지) `RejectedExecutionException`을 던지며,
 * 각 어댑터의 기존 `catch (e: Exception)` → `*GenerationFailedException` 경로로 그대로 흡수된다.
 */
@Configuration
@EnableConfigurationProperties(AiResilienceProperties::class)
class AiResilienceConfig {
    private fun isolatedExecutor(settings: AiResilienceProperties.ExecutorSettings): ExecutorService =
        ThreadPoolExecutor(
            settings.poolSize,
            settings.poolSize,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(settings.queueCapacity),
            ThreadPoolExecutor.AbortPolicy(),
        )

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
        properties: AiResilienceProperties,
    ): AiResilienceSupport =
        AiResilienceSupport(circuitBreakerRegistry, retryRegistry, timeLimiterRegistry) { isolatedExecutor(properties.executor) }
}
