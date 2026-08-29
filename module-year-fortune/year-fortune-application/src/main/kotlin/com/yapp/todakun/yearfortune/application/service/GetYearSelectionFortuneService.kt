package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneNotFoundException
import com.yapp.todakun.yearfortune.port.inbound.GetYearSelectionFortuneUseCase
import com.yapp.todakun.yearfortune.port.inbound.YearSelectionFortuneResult
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import java.util.UUID

@QueryService
class GetYearSelectionFortuneService(
    private val yearSelectionFortuneRepository: YearSelectionFortuneRepository,
) : GetYearSelectionFortuneUseCase {
    override fun getById(
        id: UUID,
        memberId: UUID,
    ): YearSelectionFortuneResult {
        val yearSelectionFortune = yearSelectionFortuneRepository.findById(id) ?: throw YearSelectionFortuneNotFoundException()

        yearSelectionFortune.validateOwner(memberId)

        return YearSelectionFortuneResult.from(yearSelectionFortune)
    }
}
