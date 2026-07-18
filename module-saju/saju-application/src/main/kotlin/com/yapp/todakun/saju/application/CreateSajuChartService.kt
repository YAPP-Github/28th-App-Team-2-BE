package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.exception.SajuInputInvalidException
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.CreateSajuChartPort
import java.time.LocalDate
import java.util.UUID

/**
 * 사주 명식 계산·저장 유스케이스. 크로스 도메인 진입점([CreateSajuChartPort]) 구현.
 * 계산(만세력 4주 → 십성·십이운성·분포)과 저장을 한 트랜잭션으로 묶는다(@CommandService).
 * auth 회원가입 트랜잭션 안에서 호출되면 전파(REQUIRED)로 같은 트랜잭션에 참여해 원자성을 보장한다.
 */
@CommandService
class CreateSajuChartService(
    private val manseryeokPort: ManseryeokPort,
    private val sajuChartRepository: SajuChartRepository,
) : CreateSajuChartPort {
    override fun create(
        userId: UUID?,
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
                userId = userId,
                isSelf = isSelf,
                name = name,
                gender = genderEnum,
                calendarType = calendarTypeEnum,
                birthDate = birthDate,
                birthTime = birthTimeEnum,
                isLeapMonth = isLeapMonth,
                fourPillars = fourPillars,
            )

        return sajuChartRepository.save(chart).id
    }
}

private inline fun <reified T : Enum<T>> String.toSajuEnum(): T =
    runCatching { enumValueOf<T>(this.trim().uppercase()) }.getOrElse { throw SajuInputInvalidException() }
