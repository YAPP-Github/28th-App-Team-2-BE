package com.yapp.todakun.dailyfortune.adapter.web.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneResult
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneStatus
import java.time.LocalDate
import java.util.UUID

/** [TodayFortuneStatus.GENERATING]일 때는 [status]만 채워지고 나머지 필드는 응답에서 생략된다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TodayFortuneResponse(
    val status: TodayFortuneStatus,
    val id: UUID?,
    val fortuneDate: LocalDate?,
    val score: Int?,
    val title: String?,
    val luckActionScores: List<LuckActionScoreResponse>?,
) {
    companion object {
        fun from(result: TodayFortuneResult) =
            TodayFortuneResponse(
                status = result.status,
                id = result.summary?.id,
                fortuneDate = result.summary?.fortuneDate,
                score = result.summary?.score,
                title = result.summary?.title,
                luckActionScores = result.summary?.luckActionScores?.map(LuckActionScoreResponse::from),
            )
    }
}
