package com.yapp.todakun.saju.adapter.manseryeok

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.exception.SajuInputInvalidException
import com.yapp.todakun.saju.exception.SajuYearOutOfRangeException
import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * manseryeok 라이브러리 대체 어댑터.
 *
 * - **일주**: 율리우스 적일수(JDN) 기반 60갑자 순환. 앵커는 manseryeok-js(KASI)와 전 범위 55,151일 일치 검증.
 * - **년주**: 서기 4년=갑자년 공식 + [SolarTermTable]의 실제 입춘 경계.
 * - **월주**: [SolarTermTable]의 실제 24절기 경계로 절월 지지를 정하고 오호둔(五虎遁)으로 월간 산출.
 * - **시주**: 일간 + 십이시진(오서둔)으로 계산.
 * - **음력 입력**: [LunarSolarConverter]로 양력 변환 후 동일 계산.
 *
 * 년/월/일주는 라이브러리 `getGapja`와 전 범위 대조 검증됨(라이브러리가 12/31 12일에서 자체 버그를 갖는
 * 지점만 제외, 그 경우는 이 어댑터가 오히려 정확). 계약([ManseryeokPort])은 유지된 채 구현만 교체 가능.
 */
@Component
class ManseryeokAdapter : ManseryeokPort {
    override fun calculate(
        birthDate: LocalDate,
        birthTime: BirthTime,
        calendarType: CalendarType,
        isLeapMonth: Boolean,
    ): FourPillars {
        if (birthDate.year !in SUPPORTED_YEARS) throw SajuYearOutOfRangeException()

        // 음력 입력이면 검증된 만세력 테이블로 양력 변환 후, 이하 계산은 양력과 동일.
        val solarDate =
            if (calendarType == CalendarType.LUNAR) {
                LunarSolarConverter.lunarToSolar(
                    lunarYear = birthDate.year,
                    lunarMonth = birthDate.monthValue,
                    lunarDay = birthDate.dayOfMonth,
                    isLeapMonth = isLeapMonth,
                ) ?: throw SajuInputInvalidException()
            } else {
                birthDate
            }

        val yearPillar = yearPillar(solarDate)
        val (monthPillar, solarTermName) = monthPillar(solarDate, yearPillar.stem)
        val dayPillar = dayPillar(solarDate)
        val hourPillar = birthTime.branch?.let { hourPillar(dayPillar.stem, it) }

        return FourPillars(
            year = yearPillar,
            month = monthPillar,
            day = dayPillar,
            hour = hourPillar,
            solarTermName = solarTermName,
        )
    }

    /** 년주: 서기 4년이 갑자년 → (해 - 4). 입춘 이전이면 전년도 간지(실제 입춘 경계는 [SolarTermTable]). */
    private fun yearPillar(solarDate: LocalDate): GanjiPillar {
        val solarYear =
            if (SolarTermTable.isBeforeIpchun(solarDate.year, monthDay(solarDate))) {
                solarDate.year - 1
            } else {
                solarDate.year
            }
        return GanjiPillar(
            stem = HeavenlyStem.ofIndex(solarYear - 4),
            branch = EarthlyBranch.ofIndex(solarYear - 4),
        )
    }

    /** 일주: JDN 기반 60갑자 순환. */
    private fun dayPillar(solarDate: LocalDate): GanjiPillar {
        val index = ((julianDayNumber(solarDate) + GAPJA_ANCHOR_OFFSET) % 60 + 60) % 60
        return GanjiPillar(
            stem = HeavenlyStem.ofIndex(index % 10),
            branch = EarthlyBranch.ofIndex(index % 12),
        )
    }

    /** 시주: 오서둔(五鼠遁). 일간으로 자시 천간을 정하고 시진 지지만큼 진행한다. */
    private fun hourPillar(
        dayStem: HeavenlyStem,
        hourBranch: EarthlyBranch,
    ): GanjiPillar {
        val jasiStemIndex = (dayStem.ordinal % 5) * 2
        return GanjiPillar(
            stem = HeavenlyStem.ofIndex(jasiStemIndex + hourBranch.ordinal),
            branch = hourBranch,
        )
    }

    /**
     * 월주: 절기 테이블로 절월 지지를 정하고, 오호둔(五虎遁)으로 월간을 정한다.
     * 월간은 년주와 같은 절기연(입춘 기준)의 년간을 쓴다.
     * @return (월주 간지, 적용 절기명)
     */
    private fun monthPillar(
        solarDate: LocalDate,
        yearStem: HeavenlyStem,
    ): Pair<GanjiPillar, String> {
        val termIndex = SolarTermTable.termIndex(solarDate.year, monthDay(solarDate))
        val branch = SolarTermTable.TERM_BRANCHES[termIndex]
        // 인월(寅月) 천간(오호둔) 후 절월 지지만큼 진행.
        val inMonthStemIndex = (yearStem.ordinal % 5) * 2 + 2
        val offsetFromIn = ((branch.ordinal - EarthlyBranch.IN.ordinal) % 12 + 12) % 12
        val stem = HeavenlyStem.ofIndex(inMonthStemIndex + offsetFromIn)
        return GanjiPillar(stem, branch) to SolarTermTable.TERM_NAMES[termIndex]
    }

    private fun monthDay(date: LocalDate): Int = date.monthValue * 100 + date.dayOfMonth

    /** 그레고리력 → 율리우스 적일수(Fliegel & Van Flandern). */
    private fun julianDayNumber(date: LocalDate): Int {
        val a = (14 - date.monthValue) / 12
        val y = date.year + 4800 - a
        val m = date.monthValue + 12 * a - 3
        return date.dayOfMonth + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    companion object {
        private val SUPPORTED_YEARS = 1900..2050

        /** 일주 60갑자 앵커. manseryeok-js(KASI)의 일주와 전 범위 55,151일 일치하도록 검증된 상수. */
        private const val GAPJA_ANCHOR_OFFSET = 49
    }
}
