package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.policy.LuckActionPolicy
import com.yapp.todakun.luck.port.inbound.GetLuckActionUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import java.util.UUID

@QueryService
class GetLuckActionService(
    private val luckActionRepository: LuckActionRepository,
) : GetLuckActionUseCase {
    override fun getById(
        id: UUID,
        memberId: UUID,
    ): LuckAction {
        val luckAction = luckActionRepository.findById(id) ?: throw LuckActionNotFoundException()

        LuckActionPolicy.validateOwner(luckAction, memberId)

        return luckAction
    }
}
