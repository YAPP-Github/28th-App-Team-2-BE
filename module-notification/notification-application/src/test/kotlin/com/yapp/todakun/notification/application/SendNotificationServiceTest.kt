package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.DeviceToken
import com.yapp.todakun.notification.Notification
import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.Platform
import com.yapp.todakun.notification.PushResult
import com.yapp.todakun.notification.port.outbound.DeviceTokenRepository
import com.yapp.todakun.notification.port.outbound.NotificationRepository
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import com.yapp.todakun.shared.GetPushConsentPort
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.ObjectProvider
import java.time.LocalTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class SendNotificationServiceTest :
    DescribeSpec(
        {
            val notificationRepository = mockk<NotificationRepository>()
            val notificationSettingRepository = mockk<NotificationSettingRepository>()
            val deviceTokenRepository = mockk<DeviceTokenRepository>()
            val pushNotificationPort = mockk<PushNotificationPort>()
            val pushConsentPort = mockk<ObjectProvider<GetPushConsentPort>>()
            val service =
                SendNotificationService(
                    notificationRepository,
                    notificationSettingRepository,
                    deviceTokenRepository,
                    pushNotificationPort,
                    pushConsentPort,
                )

            val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")
            val settingId = UUID.fromString("018f0000-0000-7000-8000-0000000000a1")

            fun command(push: Boolean = true) =
                SendNotificationCommand(
                    memberId = memberId,
                    type = NotificationType.FORTUNE,
                    title = "오늘의 운",
                    content = "확인해 보세요",
                    push = push,
                )

            beforeTest {
                // 인앱 저장은 전달받은 도메인 객체를 그대로 반환.
                every { notificationRepository.save(any()) } answers { firstArg<Notification>() }
                // 수신 동의 구현 빈이 없으면(null) 야간 여부와 무관하게 허용.
                every { pushConsentPort.getIfAvailable() } returns null
            }
            afterTest {
                clearMocks(
                    notificationRepository,
                    notificationSettingRepository,
                    deviceTokenRepository,
                    pushNotificationPort,
                    pushConsentPort,
                )
            }

            describe("send") {
                context("회원 알림 설정이 없으면(기본 OFF)") {
                    it("인앱 알림함에는 저장하지만 FCM 푸시는 발송하지 않는다") {
                        every { notificationSettingRepository.findByMemberId(memberId) } returns null

                        service.send(command())

                        verify(exactly = 1) { notificationRepository.save(any()) }
                        verify(exactly = 0) { pushNotificationPort.sendAll(any()) }
                    }
                }

                context("push=false면") {
                    it("인앱만 저장하고 토큰 조회조차 하지 않는다") {
                        service.send(command(push = false))

                        verify(exactly = 1) { notificationRepository.save(any()) }
                        verify(exactly = 0) { deviceTokenRepository.findAllByMemberId(any()) }
                    }
                }

                context("해당 알림 토글이 켜져 있고 유효 토큰이 있으면") {
                    it("FCM으로 발송하고 만료 토큰만 정리한다") {
                        val setting =
                            NotificationSetting.reconstitute(
                                settingId,
                                memberId,
                                morningReportEnabled = true,
                                morningReportTime = LocalTime.of(8, 0),
                                todakiEnabled = false,
                                luckyActionReminderEnabled = false,
                            )
                        every { notificationSettingRepository.findByMemberId(memberId) } returns setting
                        every { deviceTokenRepository.findAllByMemberId(memberId) } returns
                            listOf(
                                DeviceToken.reconstitute(settingId, memberId, "valid", Platform.IOS),
                                DeviceToken.reconstitute(settingId, memberId, "stale", Platform.ANDROID),
                            )
                        every { pushNotificationPort.sendAll(any()) } returns
                            listOf(
                                PushResult(token = "valid", success = true),
                                PushResult(token = "stale", success = false, tokenExpired = true),
                            )
                        every { deviceTokenRepository.deleteByToken("stale") } returns Unit

                        service.send(command())

                        verify(exactly = 1) { pushNotificationPort.sendAll(any()) }
                        verify(exactly = 1) { deviceTokenRepository.deleteByToken("stale") }
                        verify(exactly = 0) { deviceTokenRepository.deleteByToken("valid") }
                    }
                }
            }
        },
    )
