package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.repository.LuckActionRepository
import com.yapp.todakun.shared.GetLuckActionSummariesPort
import com.yapp.todakun.shared.LuckActionSummary
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class GetLuckActionSummariesAdapter(
    private val luckActionRepository: LuckActionRepository,
) : GetLuckActionSummariesPort {
    override fun getSummaries(
        memberId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<LuckActionSummary> =
        luckActionRepository.findAllByMemberIdBetweenFortuneDates(memberId, from, to).map {
            LuckActionSummary(
                id = it.id,
                fortuneDate = it.fortuneDate,
                fortuneCategory = it.fortuneCategory,
                title = it.title,
                achieved = it.achieved,
            )
        }
}
