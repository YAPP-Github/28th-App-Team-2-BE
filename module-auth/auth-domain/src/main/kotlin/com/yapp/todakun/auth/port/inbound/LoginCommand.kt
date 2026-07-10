package com.yapp.todakun.auth.port.inbound

import com.yapp.todakun.shared.OAuthProvider

data class LoginCommand(
    val provider: OAuthProvider,
    val oauthAccessToken: String,
)
