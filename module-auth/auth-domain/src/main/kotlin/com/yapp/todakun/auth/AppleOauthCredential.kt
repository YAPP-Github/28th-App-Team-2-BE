package com.yapp.todakun.auth

data class AppleOauthCredential(
    val providerId: String,
    val clientId: String,
    val refreshToken: String,
)
