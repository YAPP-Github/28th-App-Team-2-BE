package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.terms.MemberTermsAgreement
import com.yapp.todakun.terms.exception.TermsAgreementConflictException
import com.yapp.todakun.terms.repository.MemberTermsAgreementRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MemberTermsAgreementRepositoryAdapter(
    private val memberTermsAgreementJpaRepository: MemberTermsAgreementJpaRepository,
) : MemberTermsAgreementRepository {
    override fun findAllByMemberId(memberId: UUID): List<MemberTermsAgreement> =
        memberTermsAgreementJpaRepository.findAllByMemberId(memberId).map { it.toDomain() }

    override fun saveAll(agreements: List<MemberTermsAgreement>): List<MemberTermsAgreement> =
        try {
            // saveAllAndFlush로 즉시 플러시해, 유니크 위반이 커밋이 아닌 이 지점에서 잡히도록 한다.
            memberTermsAgreementJpaRepository
                .saveAllAndFlush(agreements.map { MemberTermsAgreementJpaEntity.fromDomain(it) })
                .map { it.toDomain() }
        } catch (_: DataIntegrityViolationException) {
            // 동시 재제출 레이스로 (member_id, terms_id) 유니크 제약이 위반된 경우 → 409로 명확히 응답.
            throw TermsAgreementConflictException()
        }
}
