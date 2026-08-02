package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.apple.AppleOauthFetcher
import com.yapp.todakun.auth.adapter.oauth.google.GoogleOauthFetcher
import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoOauthFetcher
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Component

@Component
class OauthAdapter(
    private val kakaoOauthFetcher: KakaoOauthFetcher,
    private val googleOauthFetcher: GoogleOauthFetcher,
    private val appleOauthFetcher: AppleOauthFetcher,
) : OauthPort {
    override fun fetchProfile(
        provider: OauthProvider,
        token: String,
        authorizationCode: String?,
    ): OauthMemberProfile =
        when (provider) {
            OauthProvider.KAKAO -> kakaoOauthFetcher.fetchProfile(token)
            OauthProvider.GOOGLE -> googleOauthFetcher.fetchProfile(token)
            OauthProvider.APPLE -> appleOauthFetcher.fetchProfile(token, authorizationCode)
        }
}
