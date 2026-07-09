package com.yapp.todakun.member.repository

import com.yapp.todakun.member.Member
import com.yapp.todakun.shared.OAuthProvider
import java.util.UUID

interface MemberRepository {
    fun save(member: Member): Member

    fun findById(id: UUID): Member?

    fun findIdByOauth(
        oauthProvider: OAuthProvider,
        providerId: String,
    ): UUID?
}
