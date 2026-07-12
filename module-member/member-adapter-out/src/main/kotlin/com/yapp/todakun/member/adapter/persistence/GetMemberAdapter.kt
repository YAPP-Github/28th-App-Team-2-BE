package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.GetMemberPort
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetMemberAdapter(
    private val memberRepository: MemberRepository,
) : GetMemberPort {
    override fun findIdByOauth(
        provider: OauthProvider,
        providerId: String,
    ): UUID? = memberRepository.findIdByOauth(provider, providerId)
}
