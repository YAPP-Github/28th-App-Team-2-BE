package com.yapp.todakun.auth

data class IssuedRefreshToken(
    val value: String,
    val expiresInSeconds: Long,
)
