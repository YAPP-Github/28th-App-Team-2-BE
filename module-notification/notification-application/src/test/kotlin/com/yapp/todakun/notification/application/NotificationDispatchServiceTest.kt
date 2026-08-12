package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.NoticeDispatchHistory
import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.inbound.PublishNoticeCommand
import com.yapp.todakun.notification.port.outbound.DispatchLockPort
import com.yapp.todakun.notification.port.outbound.NoticeDispatchHistoryRepository
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
import io.kotest.matchers.shouldNotBe
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
            val noticeDispatchHistoryRepository = mockk<NoticeDispatchHistoryRepository>()
            val dailyFortunePort = mockk<ObjectProvider<DailyFortuneNotificationPort>>()
            val luckyActionPort = mockk<ObjectProvider<LuckyActionNotificationPort>>()
            val service =
                NotificationDispatchService(
                    notificationSettingRepository,
                    sendNotificationPort,
                    getMemberIdsPort,
                    dispatchLockPort,
                    noticeDispatchHistoryRepository,
                    dailyFortunePort,
                    luckyActionPort,
                )

            beforeTest {
                // 락은 항상 획득에 성공해 block을 즉시 실행하는 것으로 기본 스텁.
                every { dispatchLockPort.tryRun<Unit>(any(), any()) } answers { secondArg<() -> Unit>().invoke() }
                // 공지 처리 이력은 기본적으로 선점에 성공하는 것으로 기본 스텁(멱등성 스킵 케이스는 개별 오버라이드).
                every { noticeDispatchHistoryRepository.saveIfAbsent(any()) } returns true
            }
            afterTest {
                clearMocks(
                    notificationSettingRepository,
                    sendNotificationPort,
                    getMemberIdsPort,
                    dispatchLockPort,
                    noticeDispatchHistoryRepository,
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

                    service.publish(PublishNoticeCommand("공지 제목", "공지 내용", "notice/1"))

                    verify(exactly = 2) { sendNotificationPort.send(any()) }
                    commands.map { it.type }.toSet() shouldBe setOf(NotificationType.NOTICE)
                    commands.map { it.memberId } shouldContainExactlyInAnyOrder listOf(m1, m2)
                }

                context("다른 인스턴스가 이미 공지를 발송 중이면") {
                    it("대상 조회조차 하지 않고 스킵한다") {
                        every { dispatchLockPort.tryRun<Unit>(any(), any()) } returns null

                        service.publish(PublishNoticeCommand("공지 제목", "공지 내용", "notice/1"))

                        verify(exactly = 0) { getMemberIdsPort.getMemberIds(any(), any()) }
                    }
                }

                context("이미 처리된 idempotency key면") {
                    it("회원 조회·발송을 하지 않는다") {
                        every { noticeDispatchHistoryRepository.saveIfAbsent(any()) } returns false

                        service.publish(PublishNoticeCommand("공지 제목", "공지 내용", "notice/1"))

                        verify(exactly = 0) { getMemberIdsPort.getMemberIds(any(), any()) }
                        verify(exactly = 0) { sendNotificationPort.send(any()) }
                    }
                }

                context("같은 인자로 두 번 호출하면") {
                    it("발송은 한 번뿐이다") {
                        val m1 = UUID.fromString("018f0000-0000-7000-8000-000000000006")
                        val m2 = UUID.fromString("018f0000-0000-7000-8000-000000000007")
                        every { getMemberIdsPort.getMemberIds(null, 100) } returns listOf(m1, m2)
                        every { getMemberIdsPort.getMemberIds(m2, 100) } returns emptyList()
                        every { sendNotificationPort.send(any()) } just Runs
                        // 첫 호출은 이력 선점에 성공하고, 두 번째(같은 키) 호출은 실패한다.
                        every { noticeDispatchHistoryRepository.saveIfAbsent(any()) } returnsMany listOf(true, false)

                        val command = PublishNoticeCommand("공지 제목", "공지 내용", "notice/1")
                        service.publish(command)
                        service.publish(command)

                        verify(exactly = 2) { sendNotificationPort.send(any()) }
                    }
                }

                context("명시적 idempotency key를 주면") {
                    it("내용이 같아도 다른 키로 저장한다") {
                        every { getMemberIdsPort.getMemberIds(any(), any()) } returns emptyList()
                        val captured = mutableListOf<NoticeDispatchHistory>()
                        every { noticeDispatchHistoryRepository.saveIfAbsent(capture(captured)) } returns true

                        service.publish(PublishNoticeCommand("공지 제목", "공지 내용", "notice/1", idempotencyKey = "key-a"))
                        service.publish(PublishNoticeCommand("공지 제목", "공지 내용", "notice/1", idempotencyKey = "key-b"))

                        captured.map { it.idempotencyKey }[0] shouldNotBe captured.map { it.idempotencyKey }[1]
                    }
                }
            }
        },
    )
