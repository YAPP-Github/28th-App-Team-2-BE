package com.yapp.todakun.auth.adapter.oauth.kakao

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.requireVerifiedEmail
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException
import com.yapp.todakun.shared.OauthProvider
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.requiredBody

@Component
class KakaoOauthFetcher(
    private val restClient: RestClient,
    private val kakaoOauthProperties: KakaoOauthProperties,
) {
    fun fetchProfile(accessToken: String): OauthMemberProfile {
        val response = fetchMemberInfo(accessToken)
        val email = requireVerifiedEmail(response.kakaoAccount.email, response.kakaoAccount.isEmailVerified)

        return OauthMemberProfile(
            provider = OauthProvider.KAKAO,
            providerId = response.id.toString(),
            email = email,
        )
    }

    private fun fetchMemberInfo(accessToken: String): KakaoMemberInfoResponse =
        try {
            restClient.get()
                .uri(kakaoOauthProperties.userMeUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .requiredBody<KakaoMemberInfoResponse>()
        } catch (_: RestClientResponseException) {
            throw UnauthorizedException(AuthErrorCode.OAUTH_TOKEN_INVALID)
        }
}
