package com.yapp.todakun.saju

import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 4주 간지(만세력 계산 결과)로부터 십성·십이운성·오행/십성 분포를 파생 계산하는 순수 도메인 로직.
 * 프레임워크 비의존. 십성/십이운성 판정은 라이브러리 범위 밖이라 자체 구현한다(지침 2.2절).
 */
object SajuCalculator {
    /**
     * 4주 각 기둥에 십성·십이운성을 부여한다. 일주 천간은 일원이라 천간 십성을 두지 않는다.
     * 반환 순서는 항상 [PillarType] 선언 순(년→월→일→시)이며, 이 순서가 명식 표기 순서 계약이다.
     */
    fun pillars(
        fourPillars: FourPillars,
        dayMaster: HeavenlyStem,
    ): List<SajuPillar> =
        listOf(
            pillar(PillarType.YEAR, fourPillars.year, dayMaster, isDayPillar = false),
            pillar(PillarType.MONTH, fourPillars.month, dayMaster, isDayPillar = false),
            pillar(PillarType.DAY, fourPillars.day, dayMaster, isDayPillar = true),
            pillar(PillarType.HOUR, fourPillars.hour, dayMaster, isDayPillar = false),
        )

    private fun pillar(
        type: PillarType,
        ganji: GanjiPillar,
        dayMaster: HeavenlyStem,
        isDayPillar: Boolean,
    ): SajuPillar =
        SajuPillar(
            pillarType = type,
            stem = ganji.stem,
            branch = ganji.branch,
            stemSipseong = if (isDayPillar) null else Sipseong.of(dayMaster, ganji.stem.element, ganji.stem.yinYang),
            branchSipseong = Sipseong.of(dayMaster, ganji.branch.element, ganji.branch.yinYang),
            sibiunseong = Sibiunseong.of(dayMaster, ganji.branch),
        )

    /** 8글자의 천간·지지 오행을 집계한다(시주 없이 저장된 과거 명식은 6글자). 항상 5행을 0건 포함해 반환. */
    fun ohaengDistribution(pillars: List<SajuPillar>): List<OhaengCount> {
        val elements = pillars.flatMap { listOf(it.stem.element, it.branch.element) }
        return Element.entries.map { element ->
            val count = elements.count { it == element }
            OhaengCount(element, count, percentage(count, elements.size))
        }
    }

    /** 일간(일원)을 제외한 글자의 십성을 집계한다. 항상 10종을 0건 포함해 반환. */
    fun sipseongDistribution(pillars: List<SajuPillar>): List<SipseongCount> {
        val sipseongs = pillars.flatMap { listOfNotNull(it.stemSipseong, it.branchSipseong) }
        return Sipseong.entries.map { sipseong ->
            val count = sipseongs.count { it == sipseong }
            SipseongCount(sipseong, count, percentage(count, sipseongs.size))
        }
    }

    private fun percentage(
        count: Int,
        total: Int,
    ): Double =
        if (total == 0) {
            0.0
        } else {
            BigDecimal(count * 100).divide(BigDecimal(total), 2, RoundingMode.HALF_UP).toDouble()
        }
}
