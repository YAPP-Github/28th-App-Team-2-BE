package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.auth.OAuthMemberProfile
import com.yapp.todakun.auth.token.IssuedOnboardingToken

interface OnboardingTokenPort {
    fun issue(profile: OAuthMemberProfile): IssuedOnboardingToken

    fun findProfile(token: String): OAuthMemberProfile?

    fun revoke(token: String)
}
