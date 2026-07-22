package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.port.inbound.ToggleLuckActionUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import java.util.UUID

@CommandService
class ToggleLuckActionService(
    private val luckActionRepository: LuckActionRepository,
) : ToggleLuckActionUseCase {
    override fun toggle(
        id: UUID,
        memberId: UUID,
    ): LuckAction {
        val luckAction = luckActionRepository.findById(id) ?: throw LuckActionNotFoundException()

        luckAction.validateOwner(memberId)

        return luckActionRepository.save(luckAction.toggle())
    }
}
