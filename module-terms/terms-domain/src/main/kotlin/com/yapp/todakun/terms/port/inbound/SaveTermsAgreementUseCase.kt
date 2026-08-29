package com.yapp.todakun.terms.port.inbound

import com.yapp.todakun.terms.MemberTermsAgreement

interface SaveTermsAgreementUseCase {
    fun save(command: SaveTermsAgreementCommand): List<MemberTermsAgreement>
}
