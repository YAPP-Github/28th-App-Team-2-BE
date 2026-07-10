package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.OAuthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.google.GoogleOAuthFetcher
import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoOAuthFetcher
import com.yapp.todakun.auth.port.outbound.OAuthPort
import com.yapp.todakun.shared.OAuthProvider
import org.springframework.stereotype.Component

@Component
class OAuthAdapter(
    private val kakaoOAuthFetcher: KakaoOAuthFetcher,
    private val googleOAuthFetcher: GoogleOAuthFetcher,
) : OAuthPort {
    override fun fetchProfile(
        provider: OAuthProvider,
        token: String,
    ): OAuthMemberProfile =
        when (provider) {
            OAuthProvider.KAKAO -> kakaoOAuthFetcher.fetchProfile(token)
            OAuthProvider.GOOGLE -> googleOAuthFetcher.fetchProfile(token)
        }
}
