package com.yapp.todakun.dailyfortune.port.inbound

import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.shared.LuckActionSummary
import java.time.LocalDate
import java.util.UUID

/** 오늘의 운세 히스토리 목록의 항목 하나. 상세 정보(내용·아이템) 없이 목록 표시에 필요한 값만 담는다. */
data class DailyFortuneHistorySummary(
    val id: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val luckActions: List<LuckActionSummary>,
) {
    companion object {
        fun from(
            dailyFortune: DailyFortune,
            luckActions: List<LuckActionSummary>,
        ): DailyFortuneHistorySummary =
            DailyFortuneHistorySummary(
                id = dailyFortune.id,
                fortuneDate = dailyFortune.fortuneDate,
                score = dailyFortune.score,
                title = dailyFortune.title,
                luckActions = luckActions,
            )
    }
}
