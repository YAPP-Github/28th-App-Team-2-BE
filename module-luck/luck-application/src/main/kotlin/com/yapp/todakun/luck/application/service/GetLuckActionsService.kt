package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.port.inbound.GetLuckActionsUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import java.time.LocalDate
import java.util.UUID

@QueryService
class GetLuckActionsService(
    private val luckActionRepository: LuckActionRepository,
) : GetLuckActionsUseCase {
    override fun getTodayLuckActions(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): List<LuckAction> = luckActionRepository.findAllByMemberIdAndFortuneDate(memberId, fortuneDate)
}
