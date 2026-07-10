package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.google.GoogleOauthFetcher
import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoOauthFetcher
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Component

@Component
class OauthAdapter(
    private val kakaoOauthFetcher: KakaoOauthFetcher,
    private val googleOauthFetcher: GoogleOauthFetcher,
) : OauthPort {
    override fun fetchProfile(
        provider: OauthProvider,
        token: String,
    ): OauthMemberProfile =
        when (provider) {
            OauthProvider.KAKAO -> kakaoOauthFetcher.fetchProfile(token)
            OauthProvider.GOOGLE -> googleOauthFetcher.fetchProfile(token)
        }
}
