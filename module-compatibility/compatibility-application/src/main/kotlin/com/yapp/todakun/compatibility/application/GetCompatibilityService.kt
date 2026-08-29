package com.yapp.todakun.compatibility.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.compatibility.exception.CompatibilityNotFoundException
import com.yapp.todakun.compatibility.port.inbound.CompatibilityResult
import com.yapp.todakun.compatibility.port.inbound.GetCompatibilityUseCase
import com.yapp.todakun.compatibility.port.outbound.SajuCompatibilityRepository
import com.yapp.todakun.shared.GetSajuChartNamePort
import java.util.UUID

@QueryService
class GetCompatibilityService(
    private val sajuCompatibilityRepository: SajuCompatibilityRepository,
    private val getSajuChartNamePort: GetSajuChartNamePort,
) : GetCompatibilityUseCase {
    override fun getById(
        id: UUID,
        memberId: UUID,
    ): CompatibilityResult {
        val sajuCompatibility = sajuCompatibilityRepository.findById(id) ?: throw CompatibilityNotFoundException()

        sajuCompatibility.validateOwner(memberId)

        return CompatibilityResult.from(sajuCompatibility, getSajuChartNamePort.getName(sajuCompatibility.partnerChartId))
    }
}
