package com.yapp.todakun.dayfortune.adapter.web.dto.response

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import java.time.LocalDate
import java.util.UUID

data class DaySelectionFortuneResponse(
    val id: UUID,
    val purpose: DaySelectionPurpose,
    val targetDate: LocalDate,
    val score: Int,
    val title: String,
    val content: String,
    val fortuneCategories: List<FortuneCategoryStarResponse>,
) {
    companion object {
        fun from(result: DaySelectionFortuneResult) =
            DaySelectionFortuneResponse(
                id = result.id,
                purpose = result.purpose,
                targetDate = result.targetDate,
                score = result.score,
                title = result.title,
                content = result.content,
                fortuneCategories = result.fortuneCategories.map(FortuneCategoryStarResponse::from),
            )
    }
}
