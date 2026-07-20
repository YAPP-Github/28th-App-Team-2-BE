package com.yapp.todakun.terms.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemberTermsAgreementJpaRepository : JpaRepository<MemberTermsAgreementJpaEntity, UUID> {
    fun findAllByMemberId(memberId: UUID): List<MemberTermsAgreementJpaEntity>
}
