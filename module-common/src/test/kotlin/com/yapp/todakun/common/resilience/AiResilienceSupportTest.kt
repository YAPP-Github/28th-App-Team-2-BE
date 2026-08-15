package com.yapp.todakun.common.resilience

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

private const val CIRCUIT_OPEN_TARGET = "circuit-open-target"
private const val ISOLATION_OPEN_TARGET = "isolation-open-target"
private const val ISOLATION_UNAFFECTED_TARGET = "isolation-unaffected-target"
private const val RETRY_TARGET = "retry-target"
private const val TIME_LIMITER_TARGET = "time-limiter-target"
private const val HALF_OPEN_RECOVER_TARGET = "half-open-recover-target"
private const val HALF_OPEN_REOPEN_TARGET = "half-open-reopen-target"
private const val SHARED_EXECUTOR_POOL_SIZE = 2
private const val SHARED_EXECUTOR_FAILING_TARGET = "shared-executor-failing-target"
private const val SHARED_EXECUTOR_HEALTHY_TARGET = "shared-executor-healthy-target"
private const val SHARED_EXECUTOR_FAILURE_DELAY_MILLIS = 300L
private const val ISOLATED_EXECUTOR_POOL_SIZE = 1
private const val ISOLATED_EXECUTOR_QUEUE_CAPACITY = 1
private const val ISOLATED_EXECUTOR_FAILING_TARGET = "isolated-executor-failing-target"
private const val ISOLATED_EXECUTOR_HEALTHY_TARGET = "isolated-executor-healthy-target"
private const val ISOLATED_EXECUTOR_OCCUPY_DELAY_MILLIS = 500L

// "CircuitBreaker 없이"/"인스턴스별로 전용 executor를" 두 테스트가 동일하게 쓰는 5초짜리 넉넉한 TimeLimiter.
private val GENEROUS_TIME_LIMITER_CONFIG: TimeLimiterConfig = TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build()

/**
 * 이 파일의 CircuitBreaker 테스트들은 하나같이 "슬라이딩 윈도만큼 실패하면 바로 OPEN"이 목적이라
 * minimumNumberOfCalls == slidingWindowSize, failureRateThreshold == 50f로 고정하고 나머지만 파라미터화한다.
 */
private fun quickOpenCircuitBreakerConfig(
    slidingWindowSize: Int = 2,
    waitDurationInOpenState: Duration = Duration.ofSeconds(60),
    permittedNumberOfCallsInHalfOpenState: Int? = null,
): CircuitBreakerConfig {
    val builder =
        CircuitBreakerConfig
            .custom()
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(slidingWindowSize)
            .failureRateThreshold(50f)
            .waitDurationInOpenState(waitDurationInOpenState)
    permittedNumberOfCallsInHalfOpenState?.let { builder.permittedNumberOfCallsInHalfOpenState(it) }

    return builder.build()
}

/** [count]개의 스레드에서 [target]을 동시에 호출해 [support]의 executor(풀+큐)를 채운다. 결과는 검증 대상이 아니라 무시한다. */
private fun occupyWithBackgroundCalls(
    support: AiResilienceSupport,
    target: String,
    count: Int,
    delayMillis: Long,
): List<Thread> =
    (1..count).map {
        Thread {
            runCatching {
                support.execute(target) {
                    Thread.sleep(delayMillis)
                    "unreachable"
                }
            }
        }.apply { start() }
    }

/**
 * (장애 격리 / 일시 장애 자동 복구 / 지속 장애 fail-fast / 스레드 무한 점유 방지)
 * Spring 컨텍스트 없이 resilience4j 레지스트리를 실물로 구성해 작은 임계값으로 빠르게 검증한다.
 */
class AiResilienceSupportTest : DescribeSpec({
    val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
    val retryRegistry = RetryRegistry.ofDefaults()
    val timeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
    val executor = Executors.newFixedThreadPool(4)
    val support = AiResilienceSupport(circuitBreakerRegistry, retryRegistry, timeLimiterRegistry) { executor }

    circuitBreakerRegistry.circuitBreaker(
        CIRCUIT_OPEN_TARGET,
        quickOpenCircuitBreakerConfig(slidingWindowSize = 4, permittedNumberOfCallsInHalfOpenState = 2),
    )
    circuitBreakerRegistry.circuitBreaker(ISOLATION_OPEN_TARGET, quickOpenCircuitBreakerConfig())
    val halfOpenConfig =
        quickOpenCircuitBreakerConfig(waitDurationInOpenState = Duration.ofMillis(200), permittedNumberOfCallsInHalfOpenState = 1)
    circuitBreakerRegistry.circuitBreaker(HALF_OPEN_RECOVER_TARGET, halfOpenConfig)
    circuitBreakerRegistry.circuitBreaker(HALF_OPEN_REOPEN_TARGET, halfOpenConfig)
    retryRegistry.retry(
        RETRY_TARGET,
        RetryConfig.custom<Any>().maxAttempts(2).waitDuration(Duration.ofMillis(10)).build(),
    )
    timeLimiterRegistry.timeLimiter(
        TIME_LIMITER_TARGET,
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(200)).build(),
    )

    afterSpec { executor.shutdownNow() }

    describe("execute") {
        context("연속 실패가 임계치를 넘으면") {
            it("회로가 열리고, 이후 호출은 대기 없이 즉시 CallNotPermittedException으로 실패한다") {
                repeat(4) {
                    shouldThrow<RuntimeException> {
                        support.execute(CIRCUIT_OPEN_TARGET) { throw RuntimeException("boom") }
                    }
                }

                val elapsedMillis =
                    measureTimeMillis {
                        shouldThrow<CallNotPermittedException> {
                            support.execute(CIRCUIT_OPEN_TARGET) { "unreachable" }
                        }
                    }
                elapsedMillis shouldBeLessThan 500L
            }
        }

        context("1회 실패 후 성공하면") {
            it("Retry로 복구되어 정상 결과를 반환한다") {
                val attempts = AtomicInteger(0)

                val result =
                    support.execute(RETRY_TARGET) {
                        if (attempts.getAndIncrement() == 0) throw RuntimeException("일시적 실패") else "성공"
                    }

                result shouldBe "성공"
            }
        }

        context("설정된 시간 안에 응답이 없으면") {
            it("TimeoutException으로 끊기고, 전체 소요 시간이 실제 지연시간보다 훨씬 짧게 상한된다") {
                val elapsedMillis =
                    measureTimeMillis {
                        shouldThrow<TimeoutException> {
                            support.execute(TIME_LIMITER_TARGET) {
                                Thread.sleep(5000)
                                "unreachable"
                            }
                        }
                    }
                elapsedMillis shouldBeLessThan 1000L
            }
        }

        context("한 인스턴스의 회로가 열려도") {
            it("다른 인스턴스는 영향받지 않고 정상 동작한다") {
                repeat(2) {
                    shouldThrow<RuntimeException> {
                        support.execute(ISOLATION_OPEN_TARGET) { throw RuntimeException("boom") }
                    }
                }
                shouldThrow<CallNotPermittedException> {
                    support.execute(ISOLATION_OPEN_TARGET) { "unreachable" }
                }

                val result = support.execute(ISOLATION_UNAFFECTED_TARGET) { "정상" }

                result shouldBe "정상"
            }
        }

        context("회로가 열린 뒤 대기시간이 지나 half-open 상태에서 호출이 성공하면") {
            it("회로가 닫히고 이후 호출은 정상적으로 통과한다") {
                repeat(2) {
                    shouldThrow<RuntimeException> {
                        support.execute(HALF_OPEN_RECOVER_TARGET) { throw RuntimeException("boom") }
                    }
                }
                shouldThrow<CallNotPermittedException> {
                    support.execute(HALF_OPEN_RECOVER_TARGET) { "unreachable" }
                }

                Thread.sleep(250)

                val result = support.execute(HALF_OPEN_RECOVER_TARGET) { "복구됨" }

                result shouldBe "복구됨"
                circuitBreakerRegistry.circuitBreaker(HALF_OPEN_RECOVER_TARGET).state shouldBe CircuitBreaker.State.CLOSED
            }
        }

        context("회로가 열린 뒤 대기시간이 지나 half-open 상태에서 호출이 다시 실패하면") {
            it("회로가 다시 열려 이후 호출은 즉시 차단된다") {
                repeat(2) {
                    shouldThrow<RuntimeException> {
                        support.execute(HALF_OPEN_REOPEN_TARGET) { throw RuntimeException("boom") }
                    }
                }
                shouldThrow<CallNotPermittedException> {
                    support.execute(HALF_OPEN_REOPEN_TARGET) { "unreachable" }
                }

                Thread.sleep(250)

                shouldThrow<RuntimeException> {
                    support.execute(HALF_OPEN_REOPEN_TARGET) { throw RuntimeException("여전히 장애") }
                }

                circuitBreakerRegistry.circuitBreaker(HALF_OPEN_REOPEN_TARGET).state shouldBe CircuitBreaker.State.OPEN
                shouldThrow<CallNotPermittedException> {
                    support.execute(HALF_OPEN_REOPEN_TARGET) { "unreachable" }
                }
            }
        }

        context("CircuitBreaker 없이 공유 executor에 직접 제출하면") {
            it("장애 도메인이 점유한 스레드가 빌 때까지 정상 도메인 요청이 대기한다") {
                val sharedExecutor = Executors.newFixedThreadPool(SHARED_EXECUTOR_POOL_SIZE)
                try {
                    repeat(SHARED_EXECUTOR_POOL_SIZE) {
                        CompletableFuture.supplyAsync(
                            {
                                Thread.sleep(SHARED_EXECUTOR_FAILURE_DELAY_MILLIS)
                                throw RuntimeException("장애 도메인 실패")
                            },
                            sharedExecutor,
                        )
                    }
                    Thread.sleep(50) // 풀이 장애 태스크로 채워질 시간

                    val elapsedMillis =
                        measureTimeMillis {
                            CompletableFuture.supplyAsync({ "정상 응답" }, sharedExecutor).join()
                        }

                    elapsedMillis shouldBeGreaterThan SHARED_EXECUTOR_FAILURE_DELAY_MILLIS - 100
                } finally {
                    sharedExecutor.shutdownNow()
                }
            }
        }

        context("회로가 이미 OPEN인 지속 장애 상태에서 다른 도메인이 같은 executor를 공유하면") {
            it("장애 도메인 호출이 스레드를 점유하지 않아 정상 도메인은 대기 없이 즉시 응답한다") {
                val sharedExecutor = Executors.newFixedThreadPool(SHARED_EXECUTOR_POOL_SIZE) as ThreadPoolExecutor
                try {
                    val sharedCircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
                    sharedCircuitBreakerRegistry.circuitBreaker(SHARED_EXECUTOR_FAILING_TARGET, quickOpenCircuitBreakerConfig())
                    val sharedTimeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
                    listOf(SHARED_EXECUTOR_FAILING_TARGET, SHARED_EXECUTOR_HEALTHY_TARGET).forEach { name ->
                        sharedTimeLimiterRegistry.timeLimiter(name, GENEROUS_TIME_LIMITER_CONFIG)
                    }
                    val sharedSupport =
                        AiResilienceSupport(
                            sharedCircuitBreakerRegistry,
                            RetryRegistry.ofDefaults(),
                            sharedTimeLimiterRegistry,
                        ) { sharedExecutor }

                    // 회로를 OPEN으로 워밍업(minimumNumberOfCalls만큼 실제 지연 비용을 지불)
                    repeat(2) {
                        shouldThrow<RuntimeException> {
                            sharedSupport.execute(SHARED_EXECUTOR_FAILING_TARGET) {
                                Thread.sleep(SHARED_EXECUTOR_FAILURE_DELAY_MILLIS)
                                throw RuntimeException("장애 도메인 실패")
                            }
                        }
                    }

                    // OPEN 상태에서 장애 도메인 호출을 동시에 흘려보낸다 — CB가 즉시 막아 풀을 점유하지 못해야 한다
                    val openCallThreads =
                        occupyWithBackgroundCalls(
                            sharedSupport,
                            SHARED_EXECUTOR_FAILING_TARGET,
                            SHARED_EXECUTOR_POOL_SIZE * 2,
                            SHARED_EXECUTOR_FAILURE_DELAY_MILLIS,
                        )
                    openCallThreads.forEach { it.join() }

                    // 타이밍(경계값) 대신 결정적으로 검증한다 — CB가 OPEN이면 TimeLimiter/executor 단계 자체에 도달하지 않아야 한다.
                    sharedExecutor.activeCount shouldBe 0
                    sharedExecutor.queue.size shouldBe 0

                    sharedSupport.execute(SHARED_EXECUTOR_HEALTHY_TARGET) { "정상 응답" } shouldBe "정상 응답"
                } finally {
                    sharedExecutor.shutdownNow()
                }
            }
        }

        context("인스턴스별로 전용 executor를 지연 생성하면") {
            it("한 인스턴스의 executor가 포화돼 거절당해도 다른 인스턴스는 영향받지 않는다") {
                val isolatedTimeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
                listOf(ISOLATED_EXECUTOR_FAILING_TARGET, ISOLATED_EXECUTOR_HEALTHY_TARGET).forEach { name ->
                    isolatedTimeLimiterRegistry.timeLimiter(name, GENEROUS_TIME_LIMITER_CONFIG)
                }
                val isolatedSupport =
                    AiResilienceSupport(
                        CircuitBreakerRegistry.ofDefaults(),
                        RetryRegistry.ofDefaults(),
                        isolatedTimeLimiterRegistry,
                    ) {
                        ThreadPoolExecutor(
                            ISOLATED_EXECUTOR_POOL_SIZE,
                            ISOLATED_EXECUTOR_POOL_SIZE,
                            0L,
                            TimeUnit.MILLISECONDS,
                            ArrayBlockingQueue(ISOLATED_EXECUTOR_QUEUE_CAPACITY),
                            ThreadPoolExecutor.AbortPolicy(),
                        )
                    }

                try {
                    // 풀(1) + 큐(1) = 2개까지 수용되도록 백그라운드에서 채운다.
                    val occupyingThreads =
                        occupyWithBackgroundCalls(
                            isolatedSupport,
                            ISOLATED_EXECUTOR_FAILING_TARGET,
                            ISOLATED_EXECUTOR_POOL_SIZE + ISOLATED_EXECUTOR_QUEUE_CAPACITY,
                            ISOLATED_EXECUTOR_OCCUPY_DELAY_MILLIS,
                        )
                    Thread.sleep(100) // 풀+큐가 채워질 시간

                    shouldThrow<RejectedExecutionException> {
                        isolatedSupport.execute(ISOLATED_EXECUTOR_FAILING_TARGET) { "unreachable" }
                    }

                    val result = isolatedSupport.execute(ISOLATED_EXECUTOR_HEALTHY_TARGET) { "정상" }

                    result shouldBe "정상"
                    occupyingThreads.forEach { it.join() }
                } finally {
                    isolatedSupport.destroy()
                }
            }
        }
    }
})
