package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.shared.DailyFortuneNotificationPort
import com.yapp.todakun.shared.LuckyActionNotificationPort
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.beans.factory.ObjectProvider
import java.time.LocalTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class ScheduledNotificationDispatchServiceTest :
    DescribeSpec(
        {
            val notificationSettingRepository = mockk<NotificationSettingRepository>()
            val sendNotificationPort = mockk<SendNotificationPort>()
            val dailyFortunePort = mockk<ObjectProvider<DailyFortuneNotificationPort>>()
            val luckyActionPort = mockk<ObjectProvider<LuckyActionNotificationPort>>()
            val service =
                ScheduledNotificationDispatchService(
                    notificationSettingRepository,
                    sendNotificationPort,
                    dailyFortunePort,
                    luckyActionPort,
                )

            afterTest {
                clearMocks(notificationSettingRepository, sendNotificationPort, dailyFortunePort, luckyActionPort)
            }

            fun setting(memberId: UUID) =
                NotificationSetting.reconstitute(
                    UUID.randomUUID(),
                    memberId,
                    morningReportEnabled = true,
                    morningReportTime = LocalTime.of(8, 0),
                    todakiEnabled = false,
                    luckyActionReminderEnabled = true,
                )

            describe("dispatchMorningReport") {
                context("콘텐츠 제공 빈이 없으면") {
                    it("대상 회원마다 FORTUNE 타입으로 기본 문구를 발송한다") {
                        val m1 = UUID.fromString("018f0000-0000-7000-8000-000000000001")
                        val m2 = UUID.fromString("018f0000-0000-7000-8000-000000000002")
                        every { notificationSettingRepository.findAllMorningReportTargets(LocalTime.of(8, 0)) } returns
                            listOf(setting(m1), setting(m2))
                        every { dailyFortunePort.getIfAvailable() } returns null
                        val commands = mutableListOf<SendNotificationCommand>()
                        every { sendNotificationPort.send(capture(commands)) } just Runs

                        service.dispatchMorningReport(LocalTime.of(8, 0))

                        verify(exactly = 2) { sendNotificationPort.send(any()) }
                        commands.map { it.type }.toSet() shouldBe setOf(NotificationType.FORTUNE)
                        commands.map { it.memberId } shouldContainExactlyInAnyOrder listOf(m1, m2)
                    }
                }
            }

            describe("dispatchLuckyActionReminder") {
                it("행운 액션 대상에게 LUCKY_ACTION 타입으로 발송한다") {
                    val m1 = UUID.fromString("018f0000-0000-7000-8000-000000000003")
                    every { notificationSettingRepository.findAllLuckyActionReminderTargets() } returns listOf(setting(m1))
                    every { luckyActionPort.getIfAvailable() } returns null
                    val captured = slot<SendNotificationCommand>()
                    every { sendNotificationPort.send(capture(captured)) } just Runs

                    service.dispatchLuckyActionReminder()

                    verify(exactly = 1) { sendNotificationPort.send(any()) }
                    captured.captured.type shouldBe NotificationType.LUCKY_ACTION
                }
            }
        },
    )
