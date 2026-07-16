package com.yapp.todakun.auth.fixture

import com.yapp.todakun.auth.token.IssuedAccessToken
import com.yapp.todakun.auth.token.IssuedOnboardingToken
import com.yapp.todakun.auth.token.IssuedRefreshToken

object TokenFixture {
    fun issuedAccessToken(
        value: String = "access-token-value",
        jti: String = "jti-001",
        expiresInSeconds: Long = 3600L,
    ): IssuedAccessToken =
        IssuedAccessToken(
            value = value,
            jti = jti,
            expiresInSeconds = expiresInSeconds,
        )

    fun issuedRefreshToken(
        value: String = "refresh-token-value",
        expiresInSeconds: Long = 86400L,
    ): IssuedRefreshToken =
        IssuedRefreshToken(
            value = value,
            expiresInSeconds = expiresInSeconds,
        )

    fun issuedOnboardingToken(
        value: String = "onboarding-token-value",
        expiresInSeconds: Long = 600L,
    ): IssuedOnboardingToken =
        IssuedOnboardingToken(
            value = value,
            expiresInSeconds = expiresInSeconds,
        )
}
