package com.yapp.todakun.fortune.adapter.web.dto.response

import com.yapp.todakun.fortune.port.inbound.FortuneHistorySummary
import java.time.LocalDate
import java.util.UUID

data class FortuneHistoryResponse(
    val id: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val luckActions: List<LuckActionSummaryResponse>,
) {
    companion object {
        fun from(summary: FortuneHistorySummary) =
            FortuneHistoryResponse(
                id = summary.id,
                fortuneDate = summary.fortuneDate,
                score = summary.score,
                title = summary.title,
                luckActions = summary.luckActions.map(LuckActionSummaryResponse::from),
            )
    }
}
