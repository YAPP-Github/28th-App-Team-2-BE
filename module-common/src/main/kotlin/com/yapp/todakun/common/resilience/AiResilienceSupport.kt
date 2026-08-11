package com.yapp.todakun.common.resilience

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

/**
 * 도메인별 AI 호출을 CircuitBreaker(+선택적 Retry/TimeLimiter)로 감싸 실행하는 공통 실행기.
 * 각 인스턴스([instanceName])의 CircuitBreaker/Retry/TimeLimiter는 부트스트랩(`AiResilienceConfig`)이 설정을 읽어 기동 시점에 레지스트리에 미리 등록해 둔다
 * 이 클래스는 순수 resilience4j core API만 사용해 Boot 설정 바인딩에 의존하지 않는다(`common`은 spring-tx/context만 쓴다는 프로젝트 제약 유지).
 *
 * `retryRegistry`/`timeLimiterRegistry`에 등록되지 않은 이름은 [Registry.find]가 빈 값을 반환해 해당 단계를 건너뛴다.
 * 예를 들어 chat은 이미 리액티브 스트림 자체의 Duration 타임아웃이 있어 TimeLimiter를 또 씌우면 스레드만 한 번 더 홉하므로 CircuitBreaker만 적용하고,
 * daily-fortune은 배치 Step이 이미 3회 재시도하므로 Retry를 또 씌우면 실패 시 최대 3x3회까지 AI 호출이 낭비되어 Retry를 뺀다(둘 다 `ai-resilience.instances.*` 설정에 해당 항목을 안 둬서 제어).
 */
class AiResilienceSupport(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry,
    private val timeLimiterRegistry: TimeLimiterRegistry,
    private val executor: ExecutorService,
) {
    fun <T> execute(
        instanceName: String,
        supplier: () -> T,
    ): T {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName)
        val baseSupplier = Supplier { supplier() }
        var decorated: Supplier<T> = CircuitBreaker.decorateSupplier(circuitBreaker, baseSupplier)

        retryRegistry.find(instanceName).orElse(null)?.let { retry ->
            decorated = Retry.decorateSupplier(retry, decorated)
        }

        val timeLimiter = timeLimiterRegistry.find(instanceName).orElse(null) ?: return decorated.get()
        val future = CompletableFuture.supplyAsync(decorated, executor)

        return timeLimiter.executeFutureSupplier { future }
    }
}
