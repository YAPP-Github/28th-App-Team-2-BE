package com.yapp.todakun.terms.adapter.web.dto.request

import com.yapp.todakun.terms.port.inbound.SaveTermsAgreementCommand
import com.yapp.todakun.terms.port.inbound.TermsAgreementItem
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class SaveTermsAgreementRequest(
    @field:Schema(description = "약관별 동의 내역 목록")
    @field:NotEmpty(message = "동의 내역을 최소 1건 이상 입력해 주세요.")
    @field:Valid
    val agreements: List<TermsAgreementItemRequest>,
) {
    fun toCommand(memberId: UUID) =
        SaveTermsAgreementCommand(
            memberId = memberId,
            items = agreements.map { it.toItem() },
        )
}

data class TermsAgreementItemRequest(
    @field:Schema(description = "약관 ID", example = "018f0000-0000-7000-8000-000000000001")
    @field:NotNull(message = "약관 ID를 입력해 주세요.")
    val termsId: UUID,
    @field:Schema(description = "동의 여부", example = "true")
    @field:NotNull(message = "동의 여부를 입력해 주세요.")
    val agreed: Boolean,
) {
    fun toItem() = TermsAgreementItem(termsId = termsId, agreed = agreed)
}
