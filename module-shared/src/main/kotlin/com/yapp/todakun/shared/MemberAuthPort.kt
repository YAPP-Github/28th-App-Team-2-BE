package com.yapp.todakun.shared

import java.util.UUID

interface MemberAuthPort {
    fun findMemberId(
        provider: OauthProvider,
        providerId: String,
    ): UUID?
}
