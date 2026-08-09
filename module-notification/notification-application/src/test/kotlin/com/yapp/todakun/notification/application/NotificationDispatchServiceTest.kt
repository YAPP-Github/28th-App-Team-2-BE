package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.outbound.DispatchLockPort
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.shared.DailyFortuneNotificationPort
import com.yapp.todakun.shared.GetMemberIdsPort
import com.yapp.todakun.shared.LuckyActionNotificationPort
import com.yapp.todakun.shared.NotificationPayload
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
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class NotificationDispatchServiceTest :
    DescribeSpec(
        {
            val notificationSettingRepository = mockk<NotificationSettingRepository>()
            val sendNotificationPort = mockk<SendNotificationPort>()
            val getMemberIdsPort = mockk<GetMemberIdsPort>()
            val dispatchLockPort = mockk<DispatchLockPort>()
            val dailyFortunePort = mockk<ObjectProvider<DailyFortuneNotificationPort>>()
            val luckyActionPort = mockk<ObjectProvider<LuckyActionNotificationPort>>()
            val service =
                NotificationDispatchService(
                    notificationSettingRepository,
                    sendNotificationPort,
                    getMemberIdsPort,
                    dispatchLockPort,
                    dailyFortunePort,
                    luckyActionPort,
                )

            beforeTest {
                // 락은 항상 획득에 성공해 block을 즉시 실행하는 것으로 기본 스텁.
                every { dispatchLockPort.tryRun<Unit>(any(), any()) } answers { secondArg<() -> Unit>().invoke() }
            }
            afterTest {
                clearMocks(
                    notificationSettingRepository,
                    sendNotificationPort,
                    getMemberIdsPort,
                    dispatchLockPort,
                    dailyFortunePort,
                    luckyActionPort,
                )
            }

            fun setting(memberId: UUID) =
                NotificationSetting.reconstitute(
                    Uuid.generateV7().toJavaUuid(),
                    memberId,
                    morningReportEnabled = true,
                    morningReportTime = LocalTime.of(8, 0),
                    todakiEnabled = false,
                    luckyActionReminderEnabled = true,
                    osPushPermission = null,
                )

            describe("dispatchMorningReport") {
                context("콘텐츠 포트가 실제 콘텐츠를 반환하면") {
                    it("그 콘텐츠로 FORTUNE 알림을 발송한다") {
                        val m1 = UUID.fromString("018f0000-0000-7000-8000-000000000001")
                        val settingForM1 = setting(m1)
                        every { notificationSettingRepository.findMorningReportTargets(LocalTime.of(8, 0), null, 100) } returns
                            listOf(settingForM1)
                        every { notificationSettingRepository.findMorningReportTargets(LocalTime.of(8, 0), settingForM1.id, 100) } returns
                            emptyList()
                        every { dailyFortunePort.getIfAvailable() } returns
                            mockk { every { getMorningReport(m1) } returns NotificationPayload("오늘의 운세", "80점이에요", "fortune/today") }
                        val captured = slot<SendNotificationCommand>()
                        every { sendNotificationPort.send(capture(captured)) } just Runs

                        service.dispatchMorningReport(LocalTime.of(8, 0))

                        verify(exactly = 1) { sendNotificationPort.send(any()) }
                        captured.captured.title shouldBe "오늘의 운세"
                        captured.captured.type shouldBe NotificationType.FORTUNE
                    }
                }

                context("콘텐츠 포트가 null을 반환하면(운세 미생성 등)") {
                    it("해당 회원 발송을 스킵한다") {
                        val m1 = UUID.fromString("018f0000-0000-7000-8000-000000000002")
                        val settingForM1 = setting(m1)
                        every { notificationSettingRepository.findMorningReportTargets(LocalTime.of(8, 0), null, 100) } returns
                            listOf(settingForM1)
                        every { notificationSettingRepository.findMorningReportTargets(LocalTime.of(8, 0), settingForM1.id, 100) } returns
                            emptyList()
                        every { dailyFortunePort.getIfAvailable() } returns mockk { every { getMorningReport(m1) } returns null }

                        service.dispatchMorningReport(LocalTime.of(8, 0))

                        verify(exactly = 0) { sendNotificationPort.send(any()) }
                    }
                }

                context("다른 인스턴스가 이미 락을 보유 중이면") {
                    it("대상 조회조차 하지 않고 스킵한다") {
                        every { dispatchLockPort.tryRun<Unit>(any(), any()) } returns null

                        service.dispatchMorningReport(LocalTime.of(8, 0))

                        verify(exactly = 0) { notificationSettingRepository.findMorningReportTargets(any(), any(), any()) }
                    }
                }
            }

            describe("dispatchLuckyActionReminder") {
                it("행운 액션 대상에게 LUCKY_ACTION 타입으로 발송한다") {
                    val m1 = UUID.fromString("018f0000-0000-7000-8000-000000000003")
                    val settingForM1 = setting(m1)
                    every { notificationSettingRepository.findLuckyActionReminderTargets(null, 100) } returns listOf(settingForM1)
                    every { notificationSettingRepository.findLuckyActionReminderTargets(settingForM1.id, 100) } returns emptyList()
                    every { luckyActionPort.getIfAvailable() } returns
                        mockk { every { getLuckyActionReminder(m1) } returns NotificationPayload("행운 액션", "확인해보세요", "lucky-action") }
                    val captured = slot<SendNotificationCommand>()
                    every { sendNotificationPort.send(capture(captured)) } just Runs

                    service.dispatchLuckyActionReminder()

                    verify(exactly = 1) { sendNotificationPort.send(any()) }
                    captured.captured.type shouldBe NotificationType.LUCKY_ACTION
                }
            }

            describe("publish") {
                it("전체 회원에게 NOTICE 타입으로 발송한다") {
                    val m1 = UUID.fromString("018f0000-0000-7000-8000-000000000004")
                    val m2 = UUID.fromString("018f0000-0000-7000-8000-000000000005")
                    every { getMemberIdsPort.getMemberIds(null, 100) } returns listOf(m1, m2)
                    every { getMemberIdsPort.getMemberIds(m2, 100) } returns emptyList()
                    val commands = mutableListOf<SendNotificationCommand>()
                    every { sendNotificationPort.send(capture(commands)) } just Runs

                    service.publish("공지 제목", "공지 내용", "notice/1")

                    verify(exactly = 2) { sendNotificationPort.send(any()) }
                    commands.map { it.type }.toSet() shouldBe setOf(NotificationType.NOTICE)
                    commands.map { it.memberId } shouldContainExactlyInAnyOrder listOf(m1, m2)
                }

                context("다른 인스턴스가 이미 공지를 발송 중이면") {
                    it("대상 조회조차 하지 않고 스킵한다") {
                        every { dispatchLockPort.tryRun<Unit>(any(), any()) } returns null

                        service.publish("공지 제목", "공지 내용", "notice/1")

                        verify(exactly = 0) { getMemberIdsPort.getMemberIds(any(), any()) }
                    }
                }
            }
        },
    )
