package com.yapp.todakun.auth.adapter.oauth.google

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.parseEmailVerified
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
        val email = requireVerifiedEmail(extractEmail(claims), parseEmailVerified(claims.getClaim("email_verified")))

        return OauthMemberProfile(
            provider = OauthProvider.GOOGLE,
            providerId = claims.subject,
            email = email,
        )
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

    private fun extractEmail(claims: JWTClaimsSet): String? =
        try {
            claims.getStringClaim("email")
        } catch (_: ParseException) {
            throw OauthTokenInvalidException()
        }
}
