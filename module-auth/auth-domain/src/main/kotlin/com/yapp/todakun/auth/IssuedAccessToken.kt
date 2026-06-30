package com.yapp.todakun.auth

data class IssuedAccessToken(
    val value: String,
    val jti: String,
    val expiresInSeconds: Long,
)
