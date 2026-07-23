package com.yapp.todakun.fortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.fortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.fortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.fortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.fortune.repository.DailyFortuneRepository
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
