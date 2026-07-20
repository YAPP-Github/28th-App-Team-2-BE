package com.yapp.todakun.terms.adapter.web.controller

import com.yapp.todakun.terms.adapter.web.TermsApi
import com.yapp.todakun.terms.adapter.web.dto.request.SaveTermsAgreementRequest
import com.yapp.todakun.terms.adapter.web.dto.response.TermsAgreementResponse
import com.yapp.todakun.terms.adapter.web.dto.response.TermsResponse
import com.yapp.todakun.terms.port.inbound.GetTermsUseCase
import com.yapp.todakun.terms.port.inbound.SaveTermsAgreementUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class TermsController(
    private val getTermsUseCase: GetTermsUseCase,
    private val saveTermsAgreementUseCase: SaveTermsAgreementUseCase,
) : TermsApi {
    override fun getTerms(): ResponseEntity<CommonResponse<List<TermsResponse>>> =
        CommonResponse.retrieved(getTermsUseCase.getAllTerms().map(TermsResponse::from))

    override fun saveAgreements(
        memberId: UUID,
        request: SaveTermsAgreementRequest,
    ): ResponseEntity<CommonResponse<List<TermsAgreementResponse>>> {
        val saved = saveTermsAgreementUseCase.save(request.toCommand(memberId))

        // 신규 생성/기존 갱신이 섞인 upsert이므로 201이 아닌 200으로 응답한다.
        return CommonResponse.success(saved.map(TermsAgreementResponse::from))
    }
}
