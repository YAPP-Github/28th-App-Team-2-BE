package com.yapp.todakun.auth.port.inbound

import com.yapp.todakun.auth.token.IssuedAccessToken
import com.yapp.todakun.auth.token.IssuedOnboardingToken
import com.yapp.todakun.auth.token.IssuedRefreshToken

data class LoginResult(
    val isNewMember: Boolean,
    val accessToken: IssuedAccessToken? = null,
    val refreshToken: IssuedRefreshToken? = null,
    val onboardingToken: IssuedOnboardingToken? = null,
)
