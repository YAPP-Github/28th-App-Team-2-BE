package com.yapp.todakun.fortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.fortune.fortuneHistoryRange
import com.yapp.todakun.fortune.port.inbound.DailyFortuneHistorySummary
import com.yapp.todakun.fortune.port.inbound.GetDailyFortuneHistoryUseCase
import com.yapp.todakun.fortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.GetLuckActionSummariesPort
import java.time.LocalDate
import java.util.UUID

@QueryService
class GetDailyFortuneHistoryService(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getLuckActionSummariesPort: GetLuckActionSummariesPort,
) : GetDailyFortuneHistoryUseCase {
    override fun getHistory(
        memberId: UUID,
        today: LocalDate,
        to: LocalDate,
    ): List<DailyFortuneHistorySummary> {
        val range = fortuneHistoryRange(today, to)

        val dailyFortunes = dailyFortuneRepository.findAllByMemberIdBetweenFortuneDates(memberId, range.start, range.endInclusive)
        val luckActions = getLuckActionSummariesPort.getSummaries(memberId, range.start, range.endInclusive).groupBy { it.fortuneDate }

        return dailyFortunes.map { DailyFortuneHistorySummary.from(it, luckActions[it.fortuneDate].orEmpty()) }
    }
}
