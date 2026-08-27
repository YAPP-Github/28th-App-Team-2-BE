package com.yapp.todakun.common.resilience

import com.yapp.todakun.common.exception.BusinessException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import org.springframework.beans.factory.DisposableBean
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

/**
 * 도메인별 AI 호출을 CircuitBreaker(+선택적 Retry/TimeLimiter)로 감싸 실행하는 공통 실행기.
 * 각 인스턴스([instanceName])의 CircuitBreaker/Retry/TimeLimiter는 부트스트랩(`AiResilienceConfig`)이 설정을 읽어 기동 시점에 레지스트리에 미리 등록해 둔다
 * 이 클래스는 순수 resilience4j core API만 사용해 Boot 설정 바인딩에 의존하지 않는다(`common`은 spring-tx/context만 쓴다는 프로젝트 제약 유지).
 *
 * `retryRegistry`/`timeLimiterRegistry`에 등록되지 않은 이름은 [Registry.find]가 빈 값을 반환해 해당 단계를 건너뛴다.
 * 예를 들어 chat은 이미 리액티브 스트림 자체의 Duration 타임아웃이 있어 TimeLimiter를 또 씌우면 스레드만 한 번 더 홉하므로 CircuitBreaker만 적용하고,
 * daily-fortune은 배치 Step이 이미 3회 재시도하므로 Retry를 또 씌우면 실패 시 최대 3x3회까지 AI 호출이 낭비되어 Retry를 뺀다(둘 다 `ai-resilience.instances.*` 설정에 해당 항목을 안 둬서 제어).
 *
 * 데코레이터 순서는 Retry(CircuitBreaker(TimeLimiter(call)))다. TimeLimiter를 가장 안쪽에 둬야 타임아웃이 CircuitBreaker의
 * 실패 집계에 반영되어, 회로가 OPEN된 뒤에는 executor에 작업을 제출하지 않는 fail-fast가 보장된다.
 *
 * TimeLimiter가 timeout으로 future를 취소해도 이미 실행 중인 동기 [supplier]의 스레드 인터럽트는 보장되지 않는다.
 * (Vertex AI 호출이 인터럽트에 응답하지 않으면 스레드는 실제 응답이 올 때까지 점유된 채로 남는다).
 * 그래서 모든 도메인이 스레드풀을 공유하면 한 도메인의 반복 timeout이 다른 도메인 요청까지 지연시킬 수 있어,
 * [executorFactory]로 인스턴스별 전용 executor를 지연 생성해 격리한다(부트스트랩의 `AiResilienceConfig`가 유한 큐 + 거절 정책을 가진 executor를 만들어 전달).
 */
class AiResilienceSupport(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry,
    private val timeLimiterRegistry: TimeLimiterRegistry,
    private val executorFactory: () -> ExecutorService,
) : DisposableBean {
    private val executors = ConcurrentHashMap<String, ExecutorService>()

    fun <T> execute(
        instanceName: String,
        supplier: () -> T,
    ): T {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName)
        var decorated: Supplier<T> = Supplier { supplier() }

        timeLimiterRegistry.find(instanceName).orElse(null)?.let { timeLimiter ->
            val inner = decorated
            val executor = executors.computeIfAbsent(instanceName) { executorFactory() }
            decorated = Supplier { timeLimiter.executeFutureSupplier { CompletableFuture.supplyAsync(inner, executor) } }
        }

        decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated)

        retryRegistry.find(instanceName).orElse(null)?.let { retry ->
            decorated = Retry.decorateSupplier(retry, decorated)
        }

        return decorated.get()
    }

    /**
     * [execute]를 감싸, resilience4j가 던지는 예외를 도메인별 [BusinessException]으로 변환해 다시 던진다.
     * [Exception]만 잡아 도메인 예외로 변환하고, [Error](OOM 등)는 그대로 전파한다.
     * 실패를 예외로 올리지 않고 삼키는 호출부(예: chat의 액션 카드 추출)는 변환할 예외가 없으므로 이 오버로드의 대상이 아니다.
     */
    fun <T> execute(
        instanceName: String,
        onCircuitOpen: (CallNotPermittedException) -> BusinessException,
        onTimeout: (TimeoutException) -> BusinessException,
        onFailure: (Exception) -> BusinessException,
        supplier: () -> T,
    ): T =
        try {
            execute(instanceName, supplier)
        } catch (e: CallNotPermittedException) {
            throw onCircuitOpen(e)
        } catch (e: TimeoutException) {
            throw onTimeout(e)
        } catch (e: Exception) {
            throw onFailure(e)
        }

    override fun destroy() {
        executors.values.forEach { it.shutdown() }
    }
}
