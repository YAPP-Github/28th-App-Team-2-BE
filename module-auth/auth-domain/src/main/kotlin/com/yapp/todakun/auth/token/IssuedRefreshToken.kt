package com.yapp.todakun.auth.token

data class IssuedRefreshToken(
    val value: String,
    val expiresInSeconds: Long,
)
