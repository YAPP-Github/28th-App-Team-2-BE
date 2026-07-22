package com.yapp.todakun.saju

/**
 * 명식 상세(만세력 화면)용 파생 계산. 지장간(지지→숨은 천간 정적 매핑)과 십이신살(년지 기준)은
 * DB에 저장하지 않고 응답 조립 시점에 계산한다(ERD saju_pillar에도 컬럼이 없음).
 */
object SajuDetailCalculator {
    /** 명식의 각 기둥에 지장간·십이신살을 덧붙여 상세 목록을 만든다. 신살 기준은 년지(년주 지지)다. */
    fun details(chart: SajuChart): List<PillarDetail> {
        val yearBranch = chart.pillars.first { it.pillarType == PillarType.YEAR }.branch
        return chart.pillars.map { pillar ->
            PillarDetail(
                pillar = pillar,
                jijanggan = hiddenStems(pillar.branch),
                sinsal = Sinsal.of(yearBranch, pillar.branch),
            )
        }
    }

    /** 지지의 지장간(숨은 천간). 여기(餘氣)·중기(中氣)·본기(本氣) 순으로 반환한다. */
    fun hiddenStems(branch: EarthlyBranch): List<HeavenlyStem> =
        when (branch) {
            EarthlyBranch.JA -> listOf(HeavenlyStem.IM, HeavenlyStem.GYE)
            EarthlyBranch.CHUK -> listOf(HeavenlyStem.GYE, HeavenlyStem.SIN, HeavenlyStem.GI)
            EarthlyBranch.IN -> listOf(HeavenlyStem.MU, HeavenlyStem.BYEONG, HeavenlyStem.GAP)
            EarthlyBranch.MYO -> listOf(HeavenlyStem.GAP, HeavenlyStem.EUL)
            EarthlyBranch.JIN -> listOf(HeavenlyStem.EUL, HeavenlyStem.GYE, HeavenlyStem.MU)
            EarthlyBranch.SA -> listOf(HeavenlyStem.MU, HeavenlyStem.GYEONG, HeavenlyStem.BYEONG)
            EarthlyBranch.O -> listOf(HeavenlyStem.BYEONG, HeavenlyStem.GI, HeavenlyStem.JEONG)
            EarthlyBranch.MI -> listOf(HeavenlyStem.JEONG, HeavenlyStem.EUL, HeavenlyStem.GI)
            EarthlyBranch.SIN -> listOf(HeavenlyStem.MU, HeavenlyStem.IM, HeavenlyStem.GYEONG)
            EarthlyBranch.YU -> listOf(HeavenlyStem.GYEONG, HeavenlyStem.SIN)
            EarthlyBranch.SUL -> listOf(HeavenlyStem.SIN, HeavenlyStem.JEONG, HeavenlyStem.MU)
            EarthlyBranch.HAE -> listOf(HeavenlyStem.MU, HeavenlyStem.GAP, HeavenlyStem.IM)
        }
}
