package com.yapp.todakun.member.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.MemberAuthPort
import com.yapp.todakun.shared.OAuthProvider
import java.util.UUID

@QueryService
class MemberAuthService(
    private val memberRepository: MemberRepository,
) : MemberAuthPort {
    override fun findMemberId(
        provider: OAuthProvider,
        providerId: String,
    ): UUID? = memberRepository.findIdByOauth(provider, providerId)
}
