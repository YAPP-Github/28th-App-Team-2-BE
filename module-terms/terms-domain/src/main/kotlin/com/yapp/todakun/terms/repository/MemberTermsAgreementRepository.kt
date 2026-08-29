package com.yapp.todakun.terms.repository

import com.yapp.todakun.terms.MemberTermsAgreement
import java.util.UUID

interface MemberTermsAgreementRepository {
    fun findAllByMemberId(memberId: UUID): List<MemberTermsAgreement>

    fun saveAll(agreements: List<MemberTermsAgreement>): List<MemberTermsAgreement>
}
