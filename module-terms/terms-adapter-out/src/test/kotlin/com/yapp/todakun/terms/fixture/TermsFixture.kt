package com.yapp.todakun.terms.fixture

import com.yapp.todakun.terms.MemberTermsAgreement
import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType
import java.util.UUID

object TermsFixture {
    val MEMBER_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
    val SERVICE_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000a1")
    val PRIVACY_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000a2")
    val MARKETING_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000a3")

    fun terms(
        id: UUID = SERVICE_ID,
        type: TermsType = TermsType.SERVICE,
        title: String = "서비스 이용약관",
        required: Boolean = true,
    ): Terms = Terms.reconstitute(id, type, title, required)

    fun agreement(
        id: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000b1"),
        memberId: UUID = MEMBER_ID,
        termsId: UUID = SERVICE_ID,
        agreed: Boolean = true,
    ): MemberTermsAgreement = MemberTermsAgreement.reconstitute(id, memberId, termsId, agreed)
}
