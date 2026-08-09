package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.ReplaceSelfSajuChartPort
import com.yapp.todakun.shared.event.SajuChartChangedEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 회원 본인(SELF) 사주 명식을 재계산해 교체하는 크로스 도메인 유스케이스([ReplaceSelfSajuChartPort] 구현).
 * 새 명식을 계산·저장한 뒤 기존 SELF 명식을 삭제하고 링크를 새 명식으로 갱신한다. 내 정보 수정 트랜잭션 안에서 호출된다.
 */
@CommandService
class ReplaceSelfSajuChartService(
    private val manseryeokPort: ManseryeokPort,
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : ReplaceSelfSajuChartPort {
    @ExperimentalUuidApi
    override fun replace(
        memberId: UUID,
        name: String,
        gender: String,
        calendarType: String,
        birthDate: LocalDate,
        birthTime: String,
        isLeapMonth: Boolean,
    ) {
        val fourPillars =
            manseryeokPort.calculate(
                birthDate = birthDate,
                birthTime = birthTime.toSajuEnum<BirthTime>(),
                calendarType = calendarType.toSajuEnum<CalendarType>(),
                isLeapMonth = isLeapMonth,
            )

        val newChartId =
            sajuChartRepository
                .save(
                    SajuChart.create(
                        name = name,
                        gender = gender.toSajuEnum<Gender>(),
                        calendarType = calendarType.toSajuEnum<CalendarType>(),
                        birthDate = birthDate,
                        birthTime = birthTime.toSajuEnum<BirthTime>(),
                        isLeapMonth = isLeapMonth,
                        fourPillars = fourPillars,
                    ),
                ).id

        val existing = memberSajuLinkRepository.findSelfByMemberId(memberId)
        if (existing != null) {
            sajuChartRepository.deleteById(existing.chartId)
            memberSajuLinkRepository.save(existing.updated(chartId = newChartId, relationshipType = null))
        } else {
            memberSajuLinkRepository.save(MemberSajuLink.self(memberId = memberId, chartId = newChartId))
        }

        // 명식이 바뀌었음을 알려 연도별 운세 캐시 등 파생 데이터를 정리하게 한다. AFTER_COMMIT 리스너가
        // 처리하므로 커밋 전 이벤트가 먼저 처리되는 경쟁이 생기지 않는다(이슈 #56).
        applicationEventPublisher.publishEvent(SajuChartChangedEvent(memberId))
    }
}
