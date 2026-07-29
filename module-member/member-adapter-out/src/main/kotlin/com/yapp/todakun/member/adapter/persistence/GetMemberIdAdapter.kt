package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.GetMemberIdPort
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetMemberIdAdapter(
    private val memberRepository: MemberRepository,
) : GetMemberIdPort {
    override fun findIdByOauth(
        provider: OauthProvider,
        providerId: String,
    ): UUID? = memberRepository.findIdByOauth(provider, providerId)
}
