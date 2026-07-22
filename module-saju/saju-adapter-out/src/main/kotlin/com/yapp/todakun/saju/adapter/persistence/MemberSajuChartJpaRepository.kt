package com.yapp.todakun.saju.adapter.persistence

import com.yapp.todakun.saju.SajuRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemberSajuChartJpaRepository : JpaRepository<MemberSajuChartJpaEntity, UUID> {
    fun findByMemberIdAndRole(
        memberId: UUID,
        role: SajuRole,
    ): MemberSajuChartJpaEntity?

    fun findAllByMemberIdAndRoleOrderByCreatedAtAsc(
        memberId: UUID,
        role: SajuRole,
    ): List<MemberSajuChartJpaEntity>

    fun findByIdAndMemberId(
        id: UUID,
        memberId: UUID,
    ): MemberSajuChartJpaEntity?

    fun countByMemberIdAndRole(
        memberId: UUID,
        role: SajuRole,
    ): Long

    fun findAllByMemberId(memberId: UUID): List<MemberSajuChartJpaEntity>

    fun deleteByMemberId(memberId: UUID)
}
