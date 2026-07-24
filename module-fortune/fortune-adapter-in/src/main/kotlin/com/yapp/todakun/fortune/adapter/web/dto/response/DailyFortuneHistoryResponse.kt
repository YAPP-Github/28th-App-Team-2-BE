package com.yapp.todakun.fortune.adapter.web.dto.response

import com.yapp.todakun.fortune.port.inbound.DailyFortuneHistorySummary
import java.time.LocalDate
import java.util.UUID

data class DailyFortuneHistoryResponse(
    val id: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val luckActions: List<LuckActionSummaryResponse>,
) {
    companion object {
        fun from(summary: DailyFortuneHistorySummary) =
            DailyFortuneHistoryResponse(
                id = summary.id,
                fortuneDate = summary.fortuneDate,
                score = summary.score,
                title = summary.title,
                luckActions = summary.luckActions.map(LuckActionSummaryResponse::from),
            )
    }
}
