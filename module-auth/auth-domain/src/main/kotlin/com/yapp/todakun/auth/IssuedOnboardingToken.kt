package com.yapp.todakun.auth

data class IssuedOnboardingToken(
    val value: String,
    val expiresInSeconds: Long,
)
