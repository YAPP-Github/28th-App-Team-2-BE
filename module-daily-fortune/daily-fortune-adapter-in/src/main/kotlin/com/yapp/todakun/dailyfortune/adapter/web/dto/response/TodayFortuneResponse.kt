package com.yapp.todakun.dailyfortune.adapter.web.dto.response

import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneSummary
import java.time.LocalDate
import java.util.UUID

data class TodayFortuneResponse(
    val id: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val luckActionScores: List<LuckActionScoreResponse>,
) {
    companion object {
        fun from(summary: TodayFortuneSummary) =
            TodayFortuneResponse(
                id = summary.id,
                fortuneDate = summary.fortuneDate,
                score = summary.score,
                title = summary.title,
                luckActionScores = summary.luckActionScores.map(LuckActionScoreResponse::from),
            )
    }
}
