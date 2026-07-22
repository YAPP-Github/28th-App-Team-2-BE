package com.yapp.todakun.saju.adapter.persistence

import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/** 회원-명식 소유권 링크 영속 어댑터. */
@Repository
class MemberSajuLinkRepositoryAdapter(
    private val memberSajuChartJpaRepository: MemberSajuChartJpaRepository,
) : MemberSajuLinkRepository {
    override fun save(link: MemberSajuLink): MemberSajuLink =
        memberSajuChartJpaRepository.save(MemberSajuChartJpaEntity.fromDomain(link)).toDomain()

    override fun findSelfByMemberId(memberId: UUID): MemberSajuLink? =
        memberSajuChartJpaRepository.findByMemberIdAndRole(memberId, SajuRole.SELF)?.toDomain()

    override fun findPartnersByMemberId(memberId: UUID): List<MemberSajuLink> =
        memberSajuChartJpaRepository
            .findAllByMemberIdAndRoleOrderByCreatedAtAsc(memberId, SajuRole.PARTNER)
            .map { it.toDomain() }

    override fun findByIdAndMemberId(
        id: UUID,
        memberId: UUID,
    ): MemberSajuLink? = memberSajuChartJpaRepository.findByIdAndMemberId(id, memberId)?.toDomain()

    override fun countPartnersByMemberId(memberId: UUID): Long =
        memberSajuChartJpaRepository.countByMemberIdAndRole(memberId, SajuRole.PARTNER)

    override fun deleteById(id: UUID) {
        memberSajuChartJpaRepository.deleteById(id)
    }

    override fun deleteByMemberId(memberId: UUID) {
        memberSajuChartJpaRepository.deleteByMemberId(memberId)
    }
}
