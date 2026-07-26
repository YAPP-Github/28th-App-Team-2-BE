package com.yapp.todakun.dailyfortune.port.inbound

import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.shared.LuckActionScore
import java.time.LocalDate
import java.util.UUID

/** 오늘의 운세 요약 조회 결과. 상세 정보(내용·아이템) 없이 목록/카드 표시에 필요한 값만 담는다. */
data class TodayFortuneSummary(
    val id: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val luckActionScores: List<LuckActionScore>,
) {
    companion object {
        fun from(
            dailyFortune: DailyFortune,
            luckActionScores: List<LuckActionScore>,
        ): TodayFortuneSummary =
            TodayFortuneSummary(
                id = dailyFortune.id,
                fortuneDate = dailyFortune.fortuneDate,
                score = dailyFortune.score,
                title = dailyFortune.title,
                luckActionScores = luckActionScores,
            )
    }
}
