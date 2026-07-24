package com.yapp.todakun.fortune.adapter.web.dto.response

import com.yapp.todakun.fortune.port.inbound.DailyFortuneDetail
import java.time.LocalDate
import java.util.UUID

data class FortuneDetailResponse(
    val id: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val content: String,
    val luckyItems: List<String>,
    val cautionaryItems: List<String>,
    val luckActionScores: List<LuckActionScoreResponse>,
) {
    companion object {
        fun from(detail: DailyFortuneDetail) =
            FortuneDetailResponse(
                id = detail.id,
                fortuneDate = detail.fortuneDate,
                score = detail.score,
                title = detail.title,
                content = detail.content,
                luckyItems = detail.luckyItems,
                cautionaryItems = detail.cautionaryItems,
                luckActionScores = detail.luckActionScores.map(LuckActionScoreResponse::from),
            )
    }
}
