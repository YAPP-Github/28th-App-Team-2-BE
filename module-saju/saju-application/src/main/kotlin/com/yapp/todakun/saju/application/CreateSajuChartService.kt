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
import com.yapp.todakun.shared.CreateSajuChartPort
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 사주 명식 계산·저장 유스케이스. 크로스 도메인 진입점([CreateSajuChartPort]) 구현.
 * 계산(만세력 4주 → 십성·십이운성·분포)과 저장, 그리고 회원 본인(SELF) 소유권 링크 생성을 한 트랜잭션으로 묶는다.
 * auth 회원가입 트랜잭션 안에서 호출되면 전파(REQUIRED)로 같은 트랜잭션에 참여해 원자성을 보장한다.
 */
@CommandService
class CreateSajuChartService(
    private val manseryeokPort: ManseryeokPort,
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
) : CreateSajuChartPort {
    @ExperimentalUuidApi
    override fun create(
        memberId: UUID?,
        isSelf: Boolean,
        name: String?,
        gender: String,
        calendarType: String,
        birthDate: LocalDate,
        birthTime: String,
        isLeapMonth: Boolean,
    ): UUID {
        val genderEnum = gender.toSajuEnum<Gender>()
        val calendarTypeEnum = calendarType.toSajuEnum<CalendarType>()
        val birthTimeEnum = birthTime.toSajuEnum<BirthTime>()

        val fourPillars =
            manseryeokPort.calculate(
                birthDate = birthDate,
                birthTime = birthTimeEnum,
                calendarType = calendarTypeEnum,
                isLeapMonth = isLeapMonth,
            )

        val chart =
            SajuChart.create(
                name = name,
                gender = genderEnum,
                calendarType = calendarTypeEnum,
                birthDate = birthDate,
                birthTime = birthTimeEnum,
                isLeapMonth = isLeapMonth,
                fourPillars = fourPillars,
            )

        val chartId = sajuChartRepository.save(chart).id

        // 이 크로스 도메인 포트는 회원가입 시 본인(SELF) 명식 생성에만 쓰인다.
        // 상대방(PARTNER) 명식은 saju 자체 유스케이스(RegisterPartnerSaju)로 생성한다.
        if (memberId != null && isSelf) {
            memberSajuLinkRepository.save(MemberSajuLink.self(memberId = memberId, chartId = chartId))
        }

        return chartId
    }
}
