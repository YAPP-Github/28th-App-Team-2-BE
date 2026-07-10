package com.yapp.todakun.auth.fixture

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.shared.OauthProvider
import java.util.UUID

private const val OAUTH_ACCESS_TOKEN = "test-oauth-access-token"
private const val PROVIDER_ID = "1234567890"
private const val EMAIL = "test@example.com"

object AuthFixture {
    val MEMBER_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")

    fun loginCommand(
        provider: OauthProvider = OauthProvider.KAKAO,
        oauthAccessToken: String = OAUTH_ACCESS_TOKEN,
    ): LoginCommand =
        LoginCommand(
            provider = provider,
            oauthAccessToken = oauthAccessToken,
        )

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
