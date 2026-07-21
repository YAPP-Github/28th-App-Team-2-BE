package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.port.inbound.AchieveLuckActionUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import java.util.UUID

@CommandService
class AchieveLuckActionService(
    private val luckActionRepository: LuckActionRepository,
) : AchieveLuckActionUseCase {
    override fun achieve(id: UUID): LuckAction {
        val luckAction = luckActionRepository.findById(id) ?: throw LuckActionNotFoundException()

        return luckActionRepository.save(luckAction.achieve())
    }
}
