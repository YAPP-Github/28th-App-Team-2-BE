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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
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

/**
 * (장애 격리 / 일시 장애 자동 복구 / 지속 장애 fail-fast / 스레드 무한 점유 방지)
 * Spring 컨텍스트 없이 resilience4j 레지스트리를 실물로 구성해 작은 임계값으로 빠르게 검증한다.
 */
class AiResilienceSupportTest : DescribeSpec({
    val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
    val retryRegistry = RetryRegistry.ofDefaults()
    val timeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
    val executor = Executors.newFixedThreadPool(4)
    val support = AiResilienceSupport(circuitBreakerRegistry, retryRegistry, timeLimiterRegistry, executor)

    circuitBreakerRegistry.circuitBreaker(
        CIRCUIT_OPEN_TARGET,
        CircuitBreakerConfig
            .custom()
            .slidingWindowSize(4)
            .minimumNumberOfCalls(4)
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(2)
            .build(),
    )
    circuitBreakerRegistry.circuitBreaker(
        ISOLATION_OPEN_TARGET,
        CircuitBreakerConfig
            .custom()
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .build(),
    )
    circuitBreakerRegistry.circuitBreaker(
        HALF_OPEN_RECOVER_TARGET,
        CircuitBreakerConfig
            .custom()
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofMillis(200))
            .permittedNumberOfCallsInHalfOpenState(1)
            .build(),
    )
    circuitBreakerRegistry.circuitBreaker(
        HALF_OPEN_REOPEN_TARGET,
        CircuitBreakerConfig
            .custom()
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofMillis(200))
            .permittedNumberOfCallsInHalfOpenState(1)
            .build(),
    )
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

                var elapsedMillis = 0L
                shouldThrow<CallNotPermittedException> {
                    elapsedMillis = measureTimeMillis { support.execute(CIRCUIT_OPEN_TARGET) { "unreachable" } }
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
                var elapsedMillis = 0L

                shouldThrow<TimeoutException> {
                    elapsedMillis =
                        measureTimeMillis {
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
                val sharedExecutor = Executors.newFixedThreadPool(SHARED_EXECUTOR_POOL_SIZE)
                try {
                    val sharedCircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
                    sharedCircuitBreakerRegistry.circuitBreaker(
                        SHARED_EXECUTOR_FAILING_TARGET,
                        CircuitBreakerConfig
                            .custom()
                            .slidingWindowSize(2)
                            .minimumNumberOfCalls(2)
                            .failureRateThreshold(50f)
                            .waitDurationInOpenState(Duration.ofSeconds(60))
                            .build(),
                    )
                    val sharedTimeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
                    listOf(SHARED_EXECUTOR_FAILING_TARGET, SHARED_EXECUTOR_HEALTHY_TARGET).forEach { name ->
                        sharedTimeLimiterRegistry.timeLimiter(
                            name,
                            TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build(),
                        )
                    }
                    val sharedSupport =
                        AiResilienceSupport(
                            sharedCircuitBreakerRegistry,
                            RetryRegistry.ofDefaults(),
                            sharedTimeLimiterRegistry,
                            sharedExecutor,
                        )

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
                        (1..SHARED_EXECUTOR_POOL_SIZE * 2).map {
                            Thread {
                                runCatching {
                                    sharedSupport.execute(SHARED_EXECUTOR_FAILING_TARGET) {
                                        Thread.sleep(SHARED_EXECUTOR_FAILURE_DELAY_MILLIS)
                                        "unreachable"
                                    }
                                }
                            }.apply { start() }
                        }

                    val elapsedMillis =
                        measureTimeMillis {
                            sharedSupport.execute(SHARED_EXECUTOR_HEALTHY_TARGET) { "정상 응답" }
                        }

                    openCallThreads.forEach { it.join() }
                    elapsedMillis shouldBeLessThan SHARED_EXECUTOR_FAILURE_DELAY_MILLIS / 2
                } finally {
                    sharedExecutor.shutdownNow()
                }
            }
        }
    }
})
