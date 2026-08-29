package com.yapp.todakun.terms.adapter.web

import com.yapp.todakun.terms.adapter.web.dto.request.SaveTermsAgreementRequest
import com.yapp.todakun.terms.adapter.web.dto.response.TermsAgreementResponse
import com.yapp.todakun.terms.adapter.web.dto.response.TermsResponse
import com.yapp.todakun.web.openapi.annotation.DisableSwaggerSecurity
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Terms", description = "약관 API")
interface TermsApi {
    @Operation(
        summary = "약관 목록 조회",
        description = "서비스가 제공하는 전체 약관 목록을 조회한다. 클라이언트는 이 목록의 약관 ID로 동의 내역을 제출한다.",
    )
    @DisableSwaggerSecurity
    @GetMapping("api/v1/terms")
    fun getTerms(): ResponseEntity<CommonResponse<List<TermsResponse>>>

    @Operation(
        summary = "약관 동의 내역 저장",
        description = "인증된 회원의 약관별 동의/미동의 내역을 저장한다. 필수 약관에 모두 동의하지 않으면 저장에 실패한다.",
    )
    @PostMapping("api/v1/terms/agreements")
    fun saveAgreements(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @RequestBody @Valid request: SaveTermsAgreementRequest,
    ): ResponseEntity<CommonResponse<List<TermsAgreementResponse>>>
}
