package com.yapp.todakun.auth.port.inbound

import com.yapp.todakun.shared.OauthProvider

data class LoginCommand(
    val provider: OauthProvider,
    val oauthAccessToken: String,
    val authorizationCode: String? = null,
)
