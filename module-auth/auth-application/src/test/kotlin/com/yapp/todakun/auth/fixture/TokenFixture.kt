package com.yapp.todakun.auth.fixture

import com.yapp.todakun.auth.claims.AccessTokenClaims
import com.yapp.todakun.auth.token.IssuedAccessToken
import com.yapp.todakun.auth.token.IssuedOnboardingToken
import com.yapp.todakun.auth.token.IssuedRefreshToken
import java.util.UUID

private const val JTI = "jti-001"
private const val ACCESS_TOKEN_VALUE = "access-token-value"
private const val REFRESH_TOKEN_VALUE = "refresh-token-value"
private const val ONBOARDING_TOKEN_VALUE = "onboarding-token-value"

object TokenFixture {
    fun accessTokenClaims(
        memberId: UUID,
        jti: String = JTI,
        remainingSeconds: Long = 3600L,
        isAdmin: Boolean = false,
    ): AccessTokenClaims =
        AccessTokenClaims(
            memberId = memberId,
            jti = jti,
            remainingSeconds = remainingSeconds,
            isAdmin = isAdmin,
        )

    fun issuedAccessToken(
        value: String = ACCESS_TOKEN_VALUE,
        jti: String = JTI,
        expiresInSeconds: Long = 3600L,
    ): IssuedAccessToken =
        IssuedAccessToken(
            value = value,
            jti = jti,
            expiresInSeconds = expiresInSeconds,
        )

    fun issuedRefreshToken(
        value: String = REFRESH_TOKEN_VALUE,
        expiresInSeconds: Long = 86400L,
    ): IssuedRefreshToken =
        IssuedRefreshToken(
            value = value,
            expiresInSeconds = expiresInSeconds,
        )

    fun issuedOnboardingToken(
        value: String = ONBOARDING_TOKEN_VALUE,
        expiresInSeconds: Long = 600L,
    ): IssuedOnboardingToken =
        IssuedOnboardingToken(
            value = value,
            expiresInSeconds = expiresInSeconds,
        )
}
