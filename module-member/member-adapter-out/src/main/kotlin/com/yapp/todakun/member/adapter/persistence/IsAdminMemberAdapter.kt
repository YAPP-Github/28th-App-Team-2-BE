package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.Role
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.IsAdminMemberPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IsAdminMemberAdapter(
    private val memberRepository: MemberRepository,
) : IsAdminMemberPort {
    override fun isAdmin(memberId: UUID): Boolean = memberRepository.findById(memberId)?.role == Role.ADMIN
}
