package com.yapp.todakun.auth.port

import com.yapp.todakun.auth.claims.OnboardingTokenClaims
import com.yapp.todakun.auth.token.IssuedOnboardingToken
import com.yapp.todakun.shared.OAuthProvider

interface OnboardingTokenPort {
    fun generate(
        provider: OAuthProvider,
        providerId: String,
        email: String,
    ): IssuedOnboardingToken

    fun parse(token: String): OnboardingTokenClaims
}
