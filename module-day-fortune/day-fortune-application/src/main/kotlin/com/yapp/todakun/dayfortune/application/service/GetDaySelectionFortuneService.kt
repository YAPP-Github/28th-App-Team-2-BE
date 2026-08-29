package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneNotFoundException
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import com.yapp.todakun.dayfortune.port.inbound.GetDaySelectionFortuneUseCase
import com.yapp.todakun.dayfortune.repository.DaySelectionFortuneRepository
import java.util.UUID

@QueryService
class GetDaySelectionFortuneService(
    private val daySelectionFortuneRepository: DaySelectionFortuneRepository,
) : GetDaySelectionFortuneUseCase {
    override fun getById(
        id: UUID,
        memberId: UUID,
    ): DaySelectionFortuneResult {
        val daySelectionFortune = daySelectionFortuneRepository.findById(id) ?: throw DaySelectionFortuneNotFoundException()

        daySelectionFortune.validateOwner(memberId)

        return DaySelectionFortuneResult.from(daySelectionFortune)
    }
}
