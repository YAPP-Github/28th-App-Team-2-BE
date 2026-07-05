package com.yapp.todakun.auth.port

import com.yapp.todakun.auth.OAuthMemberProfile
import com.yapp.todakun.shared.OAuthProvider

interface OAuthPort {
    fun fetchProfile(
        provider: OAuthProvider,
        token: String,
    ): OAuthMemberProfile
}
