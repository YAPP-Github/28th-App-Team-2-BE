package com.yapp.todakun.dailyfortune.port.inbound

/** 오늘의 운세 조회 결과. 다른 호출자가 이미 생성 중이면 기다리지 않고 [GENERATING] 상태로 반환한다(이슈 #90). */
data class TodayFortuneResult(
    val status: TodayFortuneStatus,
    val summary: TodayFortuneSummary?,
) {
    companion object {
        fun completed(summary: TodayFortuneSummary) = TodayFortuneResult(TodayFortuneStatus.COMPLETED, summary)

        fun generating() = TodayFortuneResult(TodayFortuneStatus.GENERATING, null)
    }
}
