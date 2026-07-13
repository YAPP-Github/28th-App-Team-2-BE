package com.yapp.todakun.auth.adapter.oauth.google

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.requireVerifiedEmail
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Component
import java.text.ParseException

@Component
class GoogleOauthFetcher(
    private val googleIdTokenProcessor: ConfigurableJWTProcessor<SecurityContext>,
    private val googleOauthProperties: GoogleOauthProperties,
) {
    fun fetchProfile(idToken: String): OauthMemberProfile {
        val claims = parseVerifiedClaims(idToken)
        val email = requireVerifiedEmail(claims.getStringClaim("email"), isEmailVerified(claims))

        return OauthMemberProfile(
            provider = OauthProvider.GOOGLE,
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
            throw OauthTokenInvalidException()
        } catch (_: BadJOSEException) {
            throw OauthTokenInvalidException()
        } catch (_: JOSEException) {
            throw OauthTokenInvalidException()
        }.also { claims ->
            if (claims.issuer !in googleOauthProperties.issuers || googleOauthProperties.clientIds.none { it in claims.audience }) {
                throw OauthTokenInvalidException()
            }
        }
}
