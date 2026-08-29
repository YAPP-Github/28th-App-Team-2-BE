package com.yapp.todakun.saju.adapter.web.controller

import com.yapp.todakun.saju.adapter.web.SajuApi
import com.yapp.todakun.saju.adapter.web.dto.request.RegisterPartnerSajuRequest
import com.yapp.todakun.saju.adapter.web.dto.request.UpdatePartnerSajuRequest
import com.yapp.todakun.saju.adapter.web.dto.response.PartnerSajuSummaryResponse
import com.yapp.todakun.saju.adapter.web.dto.response.RegisterPartnerSajuResponse
import com.yapp.todakun.saju.adapter.web.dto.response.SajuChartDetailResponse
import com.yapp.todakun.saju.port.inbound.DeletePartnerSajuUseCase
import com.yapp.todakun.saju.port.inbound.GetMySajuUseCase
import com.yapp.todakun.saju.port.inbound.GetPartnerSajuUseCase
import com.yapp.todakun.saju.port.inbound.GetPartnerSajusUseCase
import com.yapp.todakun.saju.port.inbound.RegisterPartnerSajuUseCase
import com.yapp.todakun.saju.port.inbound.UpdatePartnerSajuUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SajuController(
    private val getMySajuUseCase: GetMySajuUseCase,
    private val registerPartnerSajuUseCase: RegisterPartnerSajuUseCase,
    private val getPartnerSajusUseCase: GetPartnerSajusUseCase,
    private val getPartnerSajuUseCase: GetPartnerSajuUseCase,
    private val updatePartnerSajuUseCase: UpdatePartnerSajuUseCase,
    private val deletePartnerSajuUseCase: DeletePartnerSajuUseCase,
) : SajuApi {
    override fun getMySaju(memberId: UUID): ResponseEntity<CommonResponse<SajuChartDetailResponse>> =
        CommonResponse.retrieved(SajuChartDetailResponse.from(getMySajuUseCase.getMine(memberId)))

    override fun registerPartner(
        memberId: UUID,
        request: RegisterPartnerSajuRequest,
    ): ResponseEntity<CommonResponse<RegisterPartnerSajuResponse>> {
        val linkId = registerPartnerSajuUseCase.register(request.toCommand(memberId))

        return CommonResponse.created(RegisterPartnerSajuResponse.from(linkId))
    }

    override fun getPartners(memberId: UUID): ResponseEntity<CommonResponse<List<PartnerSajuSummaryResponse>>> =
        CommonResponse.retrieved(getPartnerSajusUseCase.getPartners(memberId).map(PartnerSajuSummaryResponse::from))

    override fun getPartner(
        memberId: UUID,
        linkId: UUID,
    ): ResponseEntity<CommonResponse<SajuChartDetailResponse>> =
        CommonResponse.retrieved(SajuChartDetailResponse.from(getPartnerSajuUseCase.getPartner(memberId, linkId)))

    override fun updatePartner(
        memberId: UUID,
        linkId: UUID,
        request: UpdatePartnerSajuRequest,
    ): ResponseEntity<CommonResponse<Unit>> {
        updatePartnerSajuUseCase.update(request.toCommand(memberId, linkId))

        return CommonResponse.updated()
    }

    override fun deletePartner(
        memberId: UUID,
        linkId: UUID,
    ): ResponseEntity<CommonResponse<Unit>> {
        deletePartnerSajuUseCase.delete(memberId, linkId)

        return CommonResponse.deleted()
    }
}
