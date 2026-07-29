package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneNotFoundException
import com.yapp.todakun.yearfortune.port.inbound.GetYearSelectionFortuneUseCase
import com.yapp.todakun.yearfortune.port.inbound.YearSelectionFortuneDetail
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import java.util.UUID

@QueryService
class GetYearSelectionFortuneService(
    private val yearSelectionFortuneRepository: YearSelectionFortuneRepository,
) : GetYearSelectionFortuneUseCase {
    override fun getByYear(
        year: Int,
        memberId: UUID,
    ): YearSelectionFortuneDetail {
        val yearSelectionFortune =
            yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) ?: throw YearSelectionFortuneNotFoundException()

        return YearSelectionFortuneDetail.from(yearSelectionFortune)
    }
}
