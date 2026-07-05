package com.yapp.todakun.auth.token

data class IssuedOnboardingToken(
    val value: String,
    val expiresInSeconds: Long,
)
