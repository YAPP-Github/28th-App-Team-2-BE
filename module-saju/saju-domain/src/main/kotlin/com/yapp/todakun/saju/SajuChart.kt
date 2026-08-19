package com.yapp.todakun.saju

import com.yapp.todakun.saju.port.outbound.FourPillars
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 사주 명식 애그리거트. 순수 계산 결과(헤더 입력값·일간 + 4주 + 오행/십성 분포)만 보유한다.
 * 소유권(회원 본인/상대 구분)은 [MemberSajuLink]가 별도로 관리한다(명식과 소유권 분리).
 * 출생시간 모름이면 입력값([birthTime]=UNKNOWN, [isTimeUnknown]=true)은 그대로 보존하되,
 * [pillars]에는 00:00(자시)로 계산한 시주가 포함된다(정책: [BirthTime.calculationBranch]).
 */
data class SajuChart(
    val id: UUID,
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

        /**
         * 영속 계층에서 저장된 값으로 명식을 복원한다(재계산 없음).
         * [pillars]는 어댑터가 넘긴 순서와 무관하게 년→월→일→시로 정규화한다.
         * 조회 쿼리에 정렬이 없어도 응답의 기둥 순서가 흔들리지 않게 하는 애그리거트 불변식이다.
         */
        @Suppress("LongParameterList")
        @JvmStatic
        fun reconstitute(
            id: UUID,
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
                name = name,
                gender = gender,
                calendarType = calendarType,
                inputDate = inputDate,
                birthTime = birthTime,
                isLeapMonth = isLeapMonth,
                isTimeUnknown = isTimeUnknown,
                solarTermName = solarTermName,
                dayMaster = dayMaster,
                pillars = pillars.sortedBy { it.pillarType.ordinal },
                ohaeng = ohaeng,
                sipseong = sipseong,
            )
    }
}
