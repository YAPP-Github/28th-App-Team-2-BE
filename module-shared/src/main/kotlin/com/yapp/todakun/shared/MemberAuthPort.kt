package com.yapp.todakun.shared

import java.util.UUID

interface MemberAuthPort {
    fun findMemberIdByOAuth(
        provider: OAuthProvider,
        providerId: String,
    ): UUID?
}
