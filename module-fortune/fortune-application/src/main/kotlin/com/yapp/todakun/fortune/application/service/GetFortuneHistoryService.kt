package com.yapp.todakun.fortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.fortune.fortuneHistoryRange
import com.yapp.todakun.fortune.port.inbound.FortuneHistorySummary
import com.yapp.todakun.fortune.port.inbound.GetFortuneHistoryUseCase
import com.yapp.todakun.fortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.GetLuckActionSummariesPort
import java.time.LocalDate
import java.util.UUID

@QueryService
class GetFortuneHistoryService(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getLuckActionSummariesPort: GetLuckActionSummariesPort,
) : GetFortuneHistoryUseCase {
    override fun getHistory(
        memberId: UUID,
        today: LocalDate,
        to: LocalDate,
    ): List<FortuneHistorySummary> {
        val range = fortuneHistoryRange(today, to)

        val dailyFortunes = dailyFortuneRepository.findAllByMemberIdBetweenFortuneDates(memberId, range.start, range.endInclusive)
        val luckActions = getLuckActionSummariesPort.getSummaries(memberId, range.start, range.endInclusive).groupBy { it.fortuneDate }

        return dailyFortunes.map { FortuneHistorySummary.from(it, luckActions[it.fortuneDate].orEmpty()) }
    }
}
