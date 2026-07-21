package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.port.inbound.FailLuckActionUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import java.util.UUID

@CommandService
class FailLuckActionService(
    private val luckActionRepository: LuckActionRepository,
) : FailLuckActionUseCase {
    override fun fail(id: UUID): LuckAction {
        val luckAction = luckActionRepository.findById(id) ?: throw LuckActionNotFoundException()

        return luckActionRepository.save(luckAction.fail())
    }
}
