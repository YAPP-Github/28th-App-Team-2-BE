package com.yapp.todakun.notification.application

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.inbound.DispatchScheduledNotificationUseCase
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationPort
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000201")
private val SETTING_ID = UUID.fromString("018f0000-0000-7000-8000-000000000202")
private val FORTUNE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000203")
private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private val MORNING_REPORT_SLOT: LocalTime = LocalTime.of(8, 0)

/**
 * 트랜잭션 없이 실행되는 NotificationDispatchService(#41 원칙)가
 * DailyFortuneNotificationAdapter를 거쳐 lazy @ElementCollection(luckyItems/cautionaryItems)에
 * 접근할 때 세션이 이미 닫혀 있어 LazyInitializationException이 나던 문제를 검증한다.
 * dispatchInChunks가 회원별 예외를 잡아 로그만 남기므로(예외가 밖으로 전파되지 않으므로),
 * "예외 없음"이 아니라 SendNotificationPort.send 호출 여부(positive assertion)로 실패를 감지한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class DispatchMorningReportTransactionBoundaryIntegrationTest(
    private val dispatchScheduledNotificationUseCase: DispatchScheduledNotificationUseCase,
    private val notificationSettingRepository: NotificationSettingRepository,
    private val dailyFortuneRepository: DailyFortuneRepository,
    transactionManager: PlatformTransactionManager,
) : DescribeSpec() {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @MockkBean
    private lateinit var sendNotificationPort: SendNotificationPort

    init {
        describe("아침 운 리포트 발송") {
            context("발송 대상 회원의 오늘의 운세가 저장돼 있으면") {
                it("LazyInitializationException 없이 알림을 발송한다") {
                    transactionTemplate.executeWithoutResult {
                        notificationSettingRepository.save(
                            NotificationSetting(
                                id = SETTING_ID,
                                memberId = MEMBER_ID,
                                morningReportEnabled = true,
                                morningReportTime = MORNING_REPORT_SLOT,
                                todakiEnabled = false,
                                luckyActionReminderEnabled = false,
                            ),
                        )
                        dailyFortuneRepository.save(
                            DailyFortune.reconstitute(
                                id = FORTUNE_ID,
                                memberId = MEMBER_ID,
                                fortuneDate = LocalDate.now(SEOUL_ZONE),
                                score = 80,
                                title = "활기찬 하루",
                                content = "오늘은 좋은 하루입니다.",
                                luckyItems = listOf("노란색", "마스크", "운동화", "셔츠", "안경"),
                                cautionaryItems = listOf("검정색", "체크무늬", "라면", "시계", "우산"),
                            ),
                        )
                    }

                    dispatchScheduledNotificationUseCase.dispatchMorningReport(MORNING_REPORT_SLOT)

                    verify(exactly = 1) {
                        sendNotificationPort.send(
                            match { it.memberId == MEMBER_ID && it.type == NotificationType.FORTUNE },
                        )
                    }
                }
            }
        }
    }
}
