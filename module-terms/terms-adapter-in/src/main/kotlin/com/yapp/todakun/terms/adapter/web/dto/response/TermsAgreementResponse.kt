package com.yapp.todakun.terms.adapter.web.dto.response

import com.yapp.todakun.terms.MemberTermsAgreement
import java.util.UUID

data class TermsAgreementResponse(
    val termsId: UUID,
    val agreed: Boolean,
) {
    companion object {
        fun from(agreement: MemberTermsAgreement) =
            TermsAgreementResponse(
                termsId = agreement.termsId,
                agreed = agreement.agreed,
            )
    }
}
