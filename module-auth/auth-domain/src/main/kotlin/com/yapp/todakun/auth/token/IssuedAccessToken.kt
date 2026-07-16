package com.yapp.todakun.auth.token

data class IssuedAccessToken(
    val value: String,
    val jti: String,
    val expiresInSeconds: Long,
)
