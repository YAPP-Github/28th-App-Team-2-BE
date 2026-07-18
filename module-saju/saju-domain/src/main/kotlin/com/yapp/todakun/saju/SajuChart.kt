package com.yapp.todakun.saju

import com.yapp.todakun.saju.port.outbound.FourPillars
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 사주 명식 애그리거트. 헤더(입력값·일간) + 4주 + 오행/십성 분포를 한 덩어리로 보유한다.
 * 본인 입력과 타인(궁합 상대 등) 입력을 모두 이 한 타입으로 처리하며 [isSelf]로 구분한다.
 */
data class SajuChart(
    val id: UUID,
    val memberId: UUID?,
    val isSelf: Boolean,
    val name: String?,
    val gender: Gender,
    val calendarType: CalendarType,
    val inputDate: LocalDate,
    val birthTime: BirthTime,
    val isLeapMonth: Boolean,
    val isTimeUnknown: Boolean,
    val solarTermName: String?,
    val dayMaster: HeavenlyStem,
    val pillars: List<SajuPillar>,
    val ohaeng: List<OhaengCount>,
    val sipseong: List<SipseongCount>,
) {
    companion object {
        /** 만세력 4주([fourPillars])와 입력값으로 명식을 생성한다. 십성·십이운성·분포는 여기서 파생 계산된다. */
        @ExperimentalUuidApi
        fun create(
            memberId: UUID?,
            isSelf: Boolean,
            name: String?,
            gender: Gender,
            calendarType: CalendarType,
            birthDate: LocalDate,
            birthTime: BirthTime,
            isLeapMonth: Boolean,
            fourPillars: FourPillars,
        ): SajuChart {
            val dayMaster = fourPillars.day.stem
            val pillars = SajuCalculator.pillars(fourPillars, dayMaster)
            return SajuChart(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                isSelf = isSelf,
                name = name,
                gender = gender,
                calendarType = calendarType,
                inputDate = birthDate,
                birthTime = birthTime,
                isLeapMonth = isLeapMonth,
                isTimeUnknown = birthTime == BirthTime.UNKNOWN,
                solarTermName = fourPillars.solarTermName,
                dayMaster = dayMaster,
                pillars = pillars,
                ohaeng = SajuCalculator.ohaengDistribution(pillars),
                sipseong = SajuCalculator.sipseongDistribution(pillars),
            )
        }

        /** 영속 계층에서 저장된 값으로 명식을 복원한다(재계산 없음). */
        @Suppress("LongParameterList")
        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID?,
            isSelf: Boolean,
            name: String?,
            gender: Gender,
            calendarType: CalendarType,
            inputDate: LocalDate,
            birthTime: BirthTime,
            isLeapMonth: Boolean,
            isTimeUnknown: Boolean,
            solarTermName: String?,
            dayMaster: HeavenlyStem,
            pillars: List<SajuPillar>,
            ohaeng: List<OhaengCount>,
            sipseong: List<SipseongCount>,
        ): SajuChart =
            SajuChart(
                id = id,
                memberId = memberId,
                isSelf = isSelf,
                name = name,
                gender = gender,
                calendarType = calendarType,
                inputDate = inputDate,
                birthTime = birthTime,
                isLeapMonth = isLeapMonth,
                isTimeUnknown = isTimeUnknown,
                solarTermName = solarTermName,
                dayMaster = dayMaster,
                pillars = pillars,
                ohaeng = ohaeng,
                sipseong = sipseong,
            )
    }
}
