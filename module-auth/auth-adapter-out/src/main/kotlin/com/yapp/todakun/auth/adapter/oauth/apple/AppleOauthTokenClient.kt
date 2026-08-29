package com.yapp.todakun.auth.adapter.oauth.apple

import com.yapp.todakun.auth.exception.OauthProviderUnavailableException
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.requiredBody

/**
 * Apple `/auth/token`(authorization code 교환) · `/auth/revoke`(계정 연결 해제) 호출.
 * 실패를 삼키지 않고 예외로 변환해 던진다 — 이 실패를 로그인/탈퇴 흐름에서 무시할지는
 * 각 정책을 아는 호출자(AppleOauthFetcher/RevokeOauthTokenAdapter)가 결정한다.
 * 4xx(invalid_grant/invalid_client 등 영구적 클라이언트 오류)는 [OauthTokenInvalidException]으로,
 * 5xx·네트워크 장애 같은 일시적 실패만 [OauthProviderUnavailableException]으로 구분해 던진다.
 */
@Component
class AppleOauthTokenClient(
    private val restClient: RestClient,
    private val appleOauthProperties: AppleOauthProperties,
    private val appleOauthClientSecretGenerator: AppleOauthClientSecretGenerator,
) {
    fun exchangeAuthorizationCode(
        clientId: String,
        authorizationCode: String,
    ): String =
        guarded {
            postForm(
                appleOauthProperties.tokenEndpoint,
                clientId,
                "grant_type" to "authorization_code",
                "code" to authorizationCode,
            ).requiredBody<AppleOauthTokenResponse>().refreshToken
        }

    fun revoke(
        clientId: String,
        refreshToken: String,
    ) {
        guarded {
            postForm(
                appleOauthProperties.revokeEndpoint,
                clientId,
                "token" to refreshToken,
                "token_type_hint" to "refresh_token",
            ).toBodilessEntity()
        }
    }

    private fun <T> guarded(block: () -> T): T =
        try {
            block()
        } catch (exception: RestClientResponseException) {
            if (exception.statusCode.is4xxClientError) {
                throw OauthTokenInvalidException()
            }
            throw OauthProviderUnavailableException()
        } catch (_: RestClientException) {
            throw OauthProviderUnavailableException()
        }

    private fun postForm(
        endpoint: String,
        clientId: String,
        vararg extraParams: Pair<String, String>,
    ): RestClient.ResponseSpec =
        restClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formBodyOf(clientId, *extraParams))
            .retrieve()

    private fun formBodyOf(
        clientId: String,
        vararg extraParams: Pair<String, String>,
    ): MultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply {
            add("client_id", clientId)
            add("client_secret", appleOauthClientSecretGenerator.generate(clientId))
            extraParams.forEach { (key, value) -> add(key, value) }
        }
}
