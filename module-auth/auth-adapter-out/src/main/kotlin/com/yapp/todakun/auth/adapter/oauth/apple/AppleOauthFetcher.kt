package com.yapp.todakun.auth.adapter.oauth.apple

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.extractIdTokenEmail
import com.yapp.todakun.auth.adapter.oauth.parseEmailVerified
import com.yapp.todakun.auth.adapter.oauth.parseIdTokenClaims
import com.yapp.todakun.auth.adapter.oauth.requireVerifiedEmail
import com.yapp.todakun.auth.exception.OauthProviderUnavailableException
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.auth.port.outbound.AppleOauthCredentialPort
import com.yapp.todakun.shared.OauthProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(AppleOauthFetcher::class.java)

@Component
class AppleOauthFetcher(
    private val appleIdTokenProcessor: ConfigurableJWTProcessor<SecurityContext>,
    private val appleOauthProperties: AppleOauthProperties,
    private val appleOauthTokenClient: AppleOauthTokenClient,
    private val appleOauthCredentialPort: AppleOauthCredentialPort,
) {
    fun fetchProfile(
        idToken: String,
        authorizationCode: String?,
    ): OauthMemberProfile {
        val claims = parseVerifiedClaims(idToken)
        val clientId = matchedClientId(claims)
        val email = requireVerifiedEmail(extractIdTokenEmail(claims), parseEmailVerified(claims.getClaim("email_verified")))

        // 로그인 자체엔 불필요하지만, App Store 심사 가이드라인(5.1.1(v))상 탈퇴 시 Apple 계정 연결도 해제(revoke)해야 한다.
        // 그때 사용할 refresh token을 authorization code 교환 시점에 미리 저장해둔다.
        // Apple은 로그인마다 매번 새 authorization code를 발급하므로, 이전 로그인에서 저장이 실패했어도 다음 로그인 때 upsert로 자연 복구된다.
        if (authorizationCode != null) {
            exchangeAndSaveCredential(clientId, claims.subject, authorizationCode)
        }

        return OauthMemberProfile(
            provider = OauthProvider.APPLE,
            providerId = claims.subject,
            email = email,
        )
    }

    // Apple 서버 장애(네트워크/일시적 불가, OauthProviderUnavailableException)로 인한 토큰 교환 실패는
    // 로그인 자체를 막지 않도록 로그만 남긴다(정책 확정). 실패 시 credential이 저장되지 않아 이후 탈퇴 시
    // revoke가 no-op이 될 수 있으나, 로그인 흐름을 우선한다.
    // invalid_grant/invalid_client 같은 영구적 클라이언트 오류(OauthTokenInvalidException)나
    // 그 외 예기치 못한 예외(예: credential 저장 중 DB 오류)는 삼키지 않고 전파해 로그인도 실패시킨다.
    private fun exchangeAndSaveCredential(
        clientId: String,
        providerId: String,
        authorizationCode: String,
    ) {
        try {
            val refreshToken = appleOauthTokenClient.exchangeAuthorizationCode(clientId, authorizationCode)
            appleOauthCredentialPort.save(providerId, clientId, refreshToken)
        } catch (exception: OauthProviderUnavailableException) {
            log.warn("Apple credential 저장 실패 - providerId: {}", providerId, exception)
        }
    }

    private fun matchedClientId(claims: JWTClaimsSet): String =
        claims.audience.firstOrNull { it in appleOauthProperties.clientIds } ?: throw OauthTokenInvalidException()

    private fun parseVerifiedClaims(idToken: String): JWTClaimsSet =
        parseIdTokenClaims(appleIdTokenProcessor, idToken).also { claims ->
            if (claims.issuer != appleOauthProperties.issuer) {
                throw OauthTokenInvalidException()
            }
        }
}
