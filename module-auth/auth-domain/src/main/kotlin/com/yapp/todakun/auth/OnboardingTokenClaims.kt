package com.yapp.todakun.auth

import com.yapp.todakun.shared.OAuthProvider

data class OnboardingTokenClaims(
    val provider: OAuthProvider,
    val providerId: String,
    val email: String,
    val remainingSeconds: Long,
)
