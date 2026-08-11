package com.yapp.todakun.common.resilience

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

private const val CIRCUIT_OPEN_TARGET = "circuit-open-target"
private const val ISOLATION_OPEN_TARGET = "isolation-open-target"
private const val ISOLATION_UNAFFECTED_TARGET = "isolation-unaffected-target"
private const val RETRY_TARGET = "retry-target"
private const val TIME_LIMITER_TARGET = "time-limiter-target"

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
    }
})
