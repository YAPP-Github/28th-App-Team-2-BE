package com.yapp.todakun.fortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.fortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.fortune.port.inbound.FortuneDetail
import com.yapp.todakun.fortune.port.inbound.GetFortuneUseCase
import com.yapp.todakun.fortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.GetLuckActionScoresPort
import java.util.UUID

@QueryService
class GetFortuneService(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getLuckActionScoresPort: GetLuckActionScoresPort,
) : GetFortuneUseCase {
    override fun getById(
        id: UUID,
        memberId: UUID,
    ): FortuneDetail {
        val dailyFortune = dailyFortuneRepository.findById(id) ?: throw DailyFortuneNotFoundException()

        dailyFortune.validateOwner(memberId)

        val luckActionScores = getLuckActionScoresPort.getScores(memberId, dailyFortune.fortuneDate)

        return FortuneDetail.from(dailyFortune, luckActionScores)
    }
}
