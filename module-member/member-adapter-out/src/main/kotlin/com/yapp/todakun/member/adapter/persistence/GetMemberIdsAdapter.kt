package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.GetMemberIdsPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetMemberIdsAdapter(
    private val memberRepository: MemberRepository,
) : GetMemberIdsPort {
    override fun getMemberIds(
        afterMemberId: UUID?,
        limit: Int,
    ): List<UUID> = memberRepository.findIdsAfter(afterMemberId, limit)
}
