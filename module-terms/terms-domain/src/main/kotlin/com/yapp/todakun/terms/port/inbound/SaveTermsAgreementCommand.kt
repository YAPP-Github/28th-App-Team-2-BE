package com.yapp.todakun.terms.port.inbound

import java.util.UUID

/**
 * 회원 한 명의 약관 동의 제출. [items]는 약관별 동의/미동의 결정 목록이다.
 */
data class SaveTermsAgreementCommand(
    val memberId: UUID,
    val items: List<TermsAgreementItem>,
)

data class TermsAgreementItem(
    val termsId: UUID,
    val agreed: Boolean,
)
