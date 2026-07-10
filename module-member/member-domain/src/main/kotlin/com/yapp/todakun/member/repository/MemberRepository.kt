package com.yapp.todakun.member.repository

import com.yapp.todakun.member.Member
import com.yapp.todakun.shared.OauthProvider
import java.util.UUID

interface MemberRepository {
    fun save(member: Member): Member

    fun findById(id: UUID): Member?

    fun findIdByOauth(
        oauthProvider: OauthProvider,
        providerId: String,
    ): UUID?
}
