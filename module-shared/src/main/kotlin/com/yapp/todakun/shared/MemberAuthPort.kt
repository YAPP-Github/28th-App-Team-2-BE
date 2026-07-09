package com.yapp.todakun.shared

import java.util.UUID

interface MemberAuthPort {
    fun findMemberId(
        provider: OAuthProvider,
        providerId: String,
    ): UUID?
}
