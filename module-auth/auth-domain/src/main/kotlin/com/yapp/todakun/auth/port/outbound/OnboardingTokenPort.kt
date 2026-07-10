package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.token.IssuedOnboardingToken

interface OnboardingTokenPort {
    fun issue(profile: OauthMemberProfile): IssuedOnboardingToken

    fun findProfile(token: String): OauthMemberProfile?

    fun revoke(token: String)
}
