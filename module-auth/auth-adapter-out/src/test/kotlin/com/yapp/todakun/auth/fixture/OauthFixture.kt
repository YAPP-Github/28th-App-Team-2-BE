package com.yapp.todakun.auth.fixture

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.shared.OauthProvider

private const val PROVIDER_ID = "1234567890"
private const val EMAIL = "test@todakun.com"

object OauthFixture {
    const val OAUTH_ACCESS_TOKEN = "test-oauth-access-token"

    fun oauthMemberProfile(
        provider: OauthProvider = OauthProvider.KAKAO,
        providerId: String = PROVIDER_ID,
        email: String = EMAIL,
    ): OauthMemberProfile =
        OauthMemberProfile(
            provider = provider,
            providerId = providerId,
            email = email,
        )
}
