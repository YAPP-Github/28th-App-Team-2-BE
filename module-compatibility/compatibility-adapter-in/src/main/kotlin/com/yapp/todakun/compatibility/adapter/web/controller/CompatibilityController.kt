package com.yapp.todakun.compatibility.adapter.web.controller

import com.yapp.todakun.compatibility.adapter.web.CompatibilityApi
import com.yapp.todakun.compatibility.adapter.web.dto.response.CompatibilityResponse
import com.yapp.todakun.compatibility.port.inbound.CreateCompatibilityUseCase
import com.yapp.todakun.compatibility.port.inbound.GetCompatibilityUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CompatibilityController(
    private val createCompatibilityUseCase: CreateCompatibilityUseCase,
    private val getCompatibilityUseCase: GetCompatibilityUseCase,
) : CompatibilityApi {
    override fun create(
        partnerLinkId: UUID,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<CompatibilityResponse>> {
        val result = createCompatibilityUseCase.create(memberId, partnerLinkId)

        return CommonResponse.created(CompatibilityResponse.from(result))
    }

    override fun getById(
        compatibilityId: UUID,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<CompatibilityResponse>> {
        val result = getCompatibilityUseCase.getById(compatibilityId, memberId)

        return CommonResponse.retrieved(CompatibilityResponse.from(result))
    }
}
