package com.yapp.todakun.auth

import com.yapp.todakun.shared.OAuthProvider

data class OAuthMemberProfile(
    val provider: OAuthProvider,
    val providerId: String,
    val email: String,
)
