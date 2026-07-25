package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.dailyfortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.dailyfortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.GetLuckActionScoresPort
import java.time.LocalDate
import java.util.UUID

@QueryService
class GetTodayFortuneService(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getLuckActionScoresPort: GetLuckActionScoresPort,
) : GetTodayFortuneUseCase {
    override fun getToday(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): TodayFortuneSummary {
        val dailyFortune =
            dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate) ?: throw DailyFortuneNotFoundException()

        val luckActionScores = getLuckActionScoresPort.getScores(memberId, fortuneDate)

        return TodayFortuneSummary.from(dailyFortune, luckActionScores)
    }
}
