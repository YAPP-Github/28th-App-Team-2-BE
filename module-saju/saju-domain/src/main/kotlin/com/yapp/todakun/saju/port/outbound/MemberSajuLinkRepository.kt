package com.yapp.todakun.saju.port.outbound

import com.yapp.todakun.saju.MemberSajuLink
import java.util.UUID

/** 회원-명식 소유권 링크 영속화 아웃바운드 포트. */
interface MemberSajuLinkRepository {
    fun save(link: MemberSajuLink): MemberSajuLink

    fun findSelfByMemberId(memberId: UUID): MemberSajuLink?

    fun findPartnersByMemberId(memberId: UUID): List<MemberSajuLink>

    fun findByIdAndMemberId(
        id: UUID,
        memberId: UUID,
    ): MemberSajuLink?

    fun countPartnersByMemberId(memberId: UUID): Long

    fun deleteById(id: UUID)

    fun deleteByMemberId(memberId: UUID)
}
