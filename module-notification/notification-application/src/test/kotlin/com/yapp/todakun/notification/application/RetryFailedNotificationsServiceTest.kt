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
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

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

            val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")

            fun failure(
                attemptCount: Int = 0,
                failedTokens: List<String> = listOf("token"),
            ) = NotificationDeliveryFailure.reconstitute(
                id = UUID.fromString("018f0000-0000-7000-8000-000000000009"),
                memberId = memberId,
                notificationId = UUID.fromString("018f0000-0000-7000-8000-00000000000a"),
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
