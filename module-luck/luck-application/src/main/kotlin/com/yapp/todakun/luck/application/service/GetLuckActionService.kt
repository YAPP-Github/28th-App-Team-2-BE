package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.port.inbound.GetLuckActionUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import java.util.UUID

@QueryService
class GetLuckActionService(
    private val luckActionRepository: LuckActionRepository,
) : GetLuckActionUseCase {
    override fun getById(id: UUID): LuckAction = luckActionRepository.findById(id) ?: throw LuckActionNotFoundException()
}
