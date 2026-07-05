package com.yapp.todakun.auth.adapter.oauth.fetcher

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.OAuthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.properties.GoogleOAuthProperties
import com.yapp.todakun.auth.adapter.oauth.requireVerifiedEmail
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException
import com.yapp.todakun.shared.OAuthProvider
import org.springframework.stereotype.Component
import java.text.ParseException

@Component
class GoogleOAuthFetcher(
    private val googleIdTokenProcessor: ConfigurableJWTProcessor<SecurityContext>,
    private val googleOAuthProperties: GoogleOAuthProperties,
) {
    fun fetchProfile(idToken: String): OAuthMemberProfile {
        val claims = parseVerifiedClaims(idToken)
        val email = requireVerifiedEmail(claims.getStringClaim("email"), isEmailVerified(claims))

        return OAuthMemberProfile(
            provider = OAuthProvider.GOOGLE,
            providerId = claims.subject,
            email = email,
        )
    }

    // 구글이 email_verified를 boolean이 아닌 문자열("true"/"false")로 내려주는 경우가 있어 타입을 직접 분기한다.
    private fun isEmailVerified(claims: JWTClaimsSet): Boolean =
        when (val emailVerified = claims.getClaim("email_verified")) {
            is Boolean -> emailVerified
            is String -> emailVerified.toBoolean()
            else -> false
        }

    private fun parseVerifiedClaims(idToken: String): JWTClaimsSet =
        try {
            googleIdTokenProcessor.process(idToken, null)
        } catch (_: ParseException) {
            throw UnauthorizedException(AuthErrorCode.OAUTH_TOKEN_INVALID)
        } catch (_: BadJOSEException) {
            throw UnauthorizedException(AuthErrorCode.OAUTH_TOKEN_INVALID)
        } catch (_: JOSEException) {
            throw UnauthorizedException(AuthErrorCode.OAUTH_TOKEN_INVALID)
        }.also { claims ->
            if (claims.issuer !in googleOAuthProperties.issuers || googleOAuthProperties.clientIds.none { it in claims.audience }) {
                throw UnauthorizedException(AuthErrorCode.OAUTH_TOKEN_INVALID)
            }
        }
}
