package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.dailyfortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.dailyfortune.port.inbound.DailyFortuneDetail
import com.yapp.todakun.dailyfortune.port.inbound.GetDailyFortuneUseCase
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.GetLuckActionScoresPort
import java.util.UUID

@QueryService
class GetDailyFortuneService(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getLuckActionScoresPort: GetLuckActionScoresPort,
) : GetDailyFortuneUseCase {
    override fun getById(
        id: UUID,
        memberId: UUID,
    ): DailyFortuneDetail {
        val dailyFortune = dailyFortuneRepository.findById(id) ?: throw DailyFortuneNotFoundException()

        dailyFortune.validateOwner(memberId)

        val luckActionScores = getLuckActionScoresPort.getScores(memberId, dailyFortune.fortuneDate)

        return DailyFortuneDetail.from(dailyFortune, luckActionScores)
    }
}
