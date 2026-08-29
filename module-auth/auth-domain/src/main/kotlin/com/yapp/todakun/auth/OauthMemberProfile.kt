package com.yapp.todakun.auth

import com.yapp.todakun.shared.OauthProvider

data class OauthMemberProfile(
    val provider: OauthProvider,
    val providerId: String,
    val email: String,
)
