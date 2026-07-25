package com.yapp.todakun.dailyfortune.port.inbound

import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.shared.LuckActionScore
import java.time.LocalDate
import java.util.UUID

/** 오늘의 운세 상세 조회 결과. 내용·행운/주의 아이템까지 포함한 전체 정보를 담는다. */
data class DailyFortuneDetail(
    val id: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val content: String,
    val luckyItems: List<String>,
    val cautionaryItems: List<String>,
    val luckActionScores: List<LuckActionScore>,
) {
    companion object {
        fun from(
            dailyFortune: DailyFortune,
            luckActionScores: List<LuckActionScore>,
        ): DailyFortuneDetail =
            DailyFortuneDetail(
                id = dailyFortune.id,
                fortuneDate = dailyFortune.fortuneDate,
                score = dailyFortune.score,
                title = dailyFortune.title,
                content = dailyFortune.content,
                luckyItems = dailyFortune.luckyItems,
                cautionaryItems = dailyFortune.cautionaryItems,
                luckActionScores = luckActionScores,
            )
    }
}
