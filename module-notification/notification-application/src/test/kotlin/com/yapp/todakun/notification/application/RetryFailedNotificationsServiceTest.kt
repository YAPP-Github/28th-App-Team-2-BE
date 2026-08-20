package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.DeviceToken
import com.yapp.todakun.notification.NotificationDeliveryFailure
import com.yapp.todakun.notification.Platform
import com.yapp.todakun.notification.PushResult
import com.yapp.todakun.notification.port.outbound.DispatchLockPort
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import com.yapp.todakun.shared.NotificationType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class RetryFailedNotificationsServiceTest :
    DescribeSpec(
        {
            val notificationTransactionalStore = mockk<NotificationTransactionalStore>()
            val pushNotificationPort = mockk<PushNotificationPort>()
            val dispatchLockPort = mockk<DispatchLockPort>()
            val notificationMetrics = mockk<NotificationMetrics>()
            val service =
                RetryFailedNotificationsService(
                    notificationTransactionalStore,
                    pushNotificationPort,
                    dispatchLockPort,
                    notificationMetrics,
                )

            val memberId = Uuid.generateV7().toJavaUuid()
            val defaultFailureId = Uuid.generateV7().toJavaUuid()
            val defaultNotificationId = Uuid.generateV7().toJavaUuid()
            val defaultMemberId = memberId

            fun failure(
                id: UUID = defaultFailureId,
                memberId: UUID = defaultMemberId,
                notificationId: UUID = defaultNotificationId,
                attemptCount: Int = 0,
                failedTokens: List<String> = listOf("token"),
            ) = NotificationDeliveryFailure.reconstitute(
                id = id,
                memberId = memberId,
                notificationId = notificationId,
                type = NotificationType.FORTUNE,
                title = "오늘의 운",
                content = "확인해 보세요",
                deepLink = "fortune/today",
                failedTokens = failedTokens,
                attemptCount = attemptCount,
                nextRetryAt = Instant.now(),
            )

            beforeTest {
                every { dispatchLockPort.tryRun<Unit>(any(), any()) } answers { secondArg<() -> Unit>().invoke() }
                every { notificationMetrics.record(any(), any(), any()) } returns Unit
            }
            afterTest {
                clearMocks(notificationTransactionalStore, pushNotificationPort, dispatchLockPort, notificationMetrics)
            }

            describe("retryDue") {
                context("디바이스 토큰이 없으면") {
                    it("재시도 없이 대기열에서 제거한다") {
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns listOf(failure())
                        every { notificationTransactionalStore.getDeviceTokens(memberId) } returns emptyList()
                        every { notificationTransactionalStore.deleteDeliveryFailure(any()) } returns Unit

                        service.retryDue()

                        verify(exactly = 0) { pushNotificationPort.sendAll(any()) }
                        verify(exactly = 1) { notificationTransactionalStore.deleteDeliveryFailure(failure().id) }
                    }
                }

                context("재시도가 성공하면") {
                    it("대기열에서 제거하고 다시 등록하지 않는다") {
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns listOf(failure())
                        every { notificationTransactionalStore.getDeviceTokens(memberId) } returns
                            listOf(DeviceToken.reconstitute(memberId, memberId, "token", Platform.IOS))
                        every { pushNotificationPort.sendAll(any()) } returns listOf(PushResult(token = "token", success = true))
                        every { notificationTransactionalStore.cleanupExpiredTokens(any()) } returns Unit
                        every { notificationTransactionalStore.deleteDeliveryFailure(any()) } returns Unit

                        service.retryDue()

                        verify(exactly = 1) { notificationTransactionalStore.deleteDeliveryFailure(failure().id) }
                        verify(exactly = 0) { notificationTransactionalStore.saveDeliveryFailure(any()) }
                    }
                }

                context("재시도가 다시 실패했고 아직 최대 횟수(3회) 미만이면") {
                    it("백오프를 늘려 다시 등록한다") {
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns
                            listOf(failure(attemptCount = 0))
                        every { notificationTransactionalStore.getDeviceTokens(memberId) } returns
                            listOf(DeviceToken.reconstitute(memberId, memberId, "token", Platform.IOS))
                        every { pushNotificationPort.sendAll(any()) } returns
                            listOf(PushResult(token = "token", success = false, tokenExpired = false))
                        every { notificationTransactionalStore.cleanupExpiredTokens(any()) } returns Unit
                        val captured = slot<NotificationDeliveryFailure>()
                        every { notificationTransactionalStore.saveDeliveryFailure(capture(captured)) } answers { firstArg() }

                        service.retryDue()

                        captured.captured.attemptCount shouldBe 1
                        verify(exactly = 0) { notificationTransactionalStore.deleteDeliveryFailure(any()) }
                        verify(exactly = 0) { notificationMetrics.record(any(), NotificationDispatchResult.RETRY_EXHAUSTED, any()) }
                    }
                }

                context("첫 재시도가 다시 실패하면") {
                    it("다음 백오프로 5분을 사용한다(1분을 반복하지 않는다)") {
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns
                            listOf(failure(attemptCount = 0))
                        every { notificationTransactionalStore.getDeviceTokens(memberId) } returns
                            listOf(DeviceToken.reconstitute(memberId, memberId, "token", Platform.IOS))
                        every { pushNotificationPort.sendAll(any()) } returns
                            listOf(PushResult(token = "token", success = false, tokenExpired = false))
                        every { notificationTransactionalStore.cleanupExpiredTokens(any()) } returns Unit
                        val captured = slot<NotificationDeliveryFailure>()
                        every { notificationTransactionalStore.saveDeliveryFailure(capture(captured)) } answers { firstArg() }

                        service.retryDue()

                        captured.captured.nextRetryAt shouldBeGreaterThan Instant.now().plus(4, ChronoUnit.MINUTES)
                        captured.captured.nextRetryAt shouldBeLessThan Instant.now().plus(6, ChronoUnit.MINUTES)
                    }
                }

                context("등록된 토큰 중 일부만 다시 실패하면") {
                    it("성공한 토큰은 빼고 실패한 토큰만 다음 재시도 대상으로 남긴다") {
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns
                            listOf(failure(failedTokens = listOf("ok-token", "bad-token")))
                        every { notificationTransactionalStore.getDeviceTokens(memberId) } returns
                            listOf(
                                DeviceToken.reconstitute(memberId, memberId, "ok-token", Platform.IOS),
                                DeviceToken.reconstitute(memberId, memberId, "bad-token", Platform.ANDROID),
                            )
                        every { pushNotificationPort.sendAll(any()) } returns
                            listOf(
                                PushResult(token = "ok-token", success = true),
                                PushResult(token = "bad-token", success = false, tokenExpired = false),
                            )
                        every { notificationTransactionalStore.cleanupExpiredTokens(any()) } returns Unit
                        val captured = slot<NotificationDeliveryFailure>()
                        every { notificationTransactionalStore.saveDeliveryFailure(capture(captured)) } answers { firstArg() }

                        service.retryDue()

                        captured.captured.failedTokens shouldContainExactlyInAnyOrder listOf("bad-token")
                    }
                }

                context("FCM 배치 호출 자체가 예외를 던지면") {
                    it("결과를 알 수 없으므로 시도한 토큰을 그대로 실패로 간주해 재예약한다") {
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns
                            listOf(failure(attemptCount = 0))
                        every { notificationTransactionalStore.getDeviceTokens(memberId) } returns
                            listOf(DeviceToken.reconstitute(memberId, memberId, "token", Platform.IOS))
                        every { pushNotificationPort.sendAll(any()) } throws RuntimeException("네트워크 오류")
                        val captured = slot<NotificationDeliveryFailure>()
                        every { notificationTransactionalStore.saveDeliveryFailure(capture(captured)) } answers { firstArg() }

                        service.retryDue()

                        captured.captured.attemptCount shouldBe 1
                        captured.captured.failedTokens shouldContainExactlyInAnyOrder listOf("token")
                        verify(exactly = 0) { notificationTransactionalStore.cleanupExpiredTokens(any()) }
                    }
                }

                context("이미 3회 재시도했는데 또 실패하면") {
                    it("재시도 소진 지표를 기록하고 대기열에서 제거한다") {
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns
                            listOf(failure(attemptCount = 2))
                        every { notificationTransactionalStore.getDeviceTokens(memberId) } returns
                            listOf(DeviceToken.reconstitute(memberId, memberId, "token", Platform.IOS))
                        every { pushNotificationPort.sendAll(any()) } returns
                            listOf(PushResult(token = "token", success = false, tokenExpired = false, errorCode = "UNAVAILABLE"))
                        every { notificationTransactionalStore.cleanupExpiredTokens(any()) } returns Unit
                        every { notificationTransactionalStore.deleteDeliveryFailure(any()) } returns Unit

                        service.retryDue()

                        verify(
                            exactly = 1,
                        ) {
                            notificationMetrics.record(NotificationType.FORTUNE, NotificationDispatchResult.RETRY_EXHAUSTED, "UNAVAILABLE")
                        }
                        verify(exactly = 1) { notificationTransactionalStore.deleteDeliveryFailure(any()) }
                        verify(exactly = 0) { notificationTransactionalStore.saveDeliveryFailure(any()) }
                    }
                }

                context("여러 건 중 일부가 예외를 던져도(#81 병렬 처리)") {
                    it("나머지 건의 재시도는 계속 진행하고, 재시도는 실제로 동시에 진행된다") {
                        val memberA = Uuid.generateV7().toJavaUuid()
                        val memberB = Uuid.generateV7().toJavaUuid()
                        val failureA =
                            failure(id = Uuid.generateV7().toJavaUuid(), memberId = memberA, failedTokens = listOf("token-a"))
                        val failureB =
                            failure(id = Uuid.generateV7().toJavaUuid(), memberId = memberB, failedTokens = listOf("token-b"))
                        every { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) } returns listOf(failureA, failureB)
                        // 두 건 다 100ms 동안 "진행 중" 상태를 유지해, 순차 실행이면 겹칠 수 없는 구간을 만든다.
                        val inFlight = AtomicInteger(0)
                        val maxInFlight = AtomicInteger(0)

                        fun trackInFlight() {
                            val current = inFlight.incrementAndGet()
                            maxInFlight.updateAndGet { prev -> maxOf(prev, current) }
                            Thread.sleep(100)
                            inFlight.decrementAndGet()
                        }
                        every { notificationTransactionalStore.getDeviceTokens(memberA) } answers {
                            trackInFlight()
                            throw RuntimeException("DB 오류")
                        }
                        every { notificationTransactionalStore.getDeviceTokens(memberB) } answers {
                            trackInFlight()
                            listOf(DeviceToken.reconstitute(memberB, memberB, "token-b", Platform.IOS))
                        }
                        every { pushNotificationPort.sendAll(any()) } returns listOf(PushResult(token = "token-b", success = true))
                        every { notificationTransactionalStore.cleanupExpiredTokens(any()) } returns Unit
                        every { notificationTransactionalStore.deleteDeliveryFailure(failureB.id) } returns Unit

                        service.retryDue()

                        verify(exactly = 1) { pushNotificationPort.sendAll(any()) }
                        verify(exactly = 1) { notificationTransactionalStore.deleteDeliveryFailure(failureB.id) }
                        verify(exactly = 0) { notificationTransactionalStore.deleteDeliveryFailure(failureA.id) }
                        verify(exactly = 0) { notificationTransactionalStore.saveDeliveryFailure(any()) }
                        // 순차 forEach로 회귀하면 A가 끝나야 B가 시작돼 maxInFlight가 1을 넘지 못한다.
                        maxInFlight.get() shouldBeGreaterThanOrEqual 2
                    }
                }

                context("다른 인스턴스가 이미 락을 보유 중이면") {
                    it("대기열 조회조차 하지 않고 스킵한다") {
                        every { dispatchLockPort.tryRun<Unit>(any(), any()) } returns null

                        service.retryDue()

                        verify(exactly = 0) { notificationTransactionalStore.findDueDeliveryFailures(any(), any()) }
                    }
                }
            }
        },
    )
