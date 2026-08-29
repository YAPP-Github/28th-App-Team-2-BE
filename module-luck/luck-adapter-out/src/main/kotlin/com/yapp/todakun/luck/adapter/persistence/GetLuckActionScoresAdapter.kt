package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.repository.LuckActionRepository
import com.yapp.todakun.shared.GetLuckActionScoresPort
import com.yapp.todakun.shared.LuckActionScore
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class GetLuckActionScoresAdapter(
    private val luckActionRepository: LuckActionRepository,
) : GetLuckActionScoresPort {
    override fun getScores(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): List<LuckActionScore> =
        luckActionRepository.findAllByMemberIdAndFortuneDate(memberId, fortuneDate).map {
            LuckActionScore(id = it.id, fortuneCategory = it.fortuneCategory, score = it.score)
        }
}
