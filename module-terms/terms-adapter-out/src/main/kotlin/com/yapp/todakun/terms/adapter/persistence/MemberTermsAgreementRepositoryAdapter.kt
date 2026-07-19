package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.terms.MemberTermsAgreement
import com.yapp.todakun.terms.repository.MemberTermsAgreementRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MemberTermsAgreementRepositoryAdapter(
    private val memberTermsAgreementJpaRepository: MemberTermsAgreementJpaRepository,
) : MemberTermsAgreementRepository {
    override fun findAllByMemberId(memberId: UUID): List<MemberTermsAgreement> =
        memberTermsAgreementJpaRepository.findAllByMemberId(memberId).map { it.toDomain() }

    override fun saveAll(agreements: List<MemberTermsAgreement>): List<MemberTermsAgreement> =
        memberTermsAgreementJpaRepository
            .saveAll(agreements.map { MemberTermsAgreementJpaEntity.fromDomain(it) })
            .map { it.toDomain() }
}
