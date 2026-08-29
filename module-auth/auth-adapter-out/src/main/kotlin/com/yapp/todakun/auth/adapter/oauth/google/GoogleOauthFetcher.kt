package com.yapp.todakun.auth.adapter.oauth.google

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.oauth.extractIdTokenEmail
import com.yapp.todakun.auth.adapter.oauth.parseEmailVerified
import com.yapp.todakun.auth.adapter.oauth.parseIdTokenClaims
import com.yapp.todakun.auth.adapter.oauth.requireVerifiedEmail
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Component

@Component
class GoogleOauthFetcher(
    private val googleIdTokenProcessor: ConfigurableJWTProcessor<SecurityContext>,
    private val googleOauthProperties: GoogleOauthProperties,
) {
    fun fetchProfile(idToken: String): OauthMemberProfile {
        val claims = parseVerifiedClaims(idToken)
        val email = requireVerifiedEmail(extractIdTokenEmail(claims), parseEmailVerified(claims.getClaim("email_verified")))

        return OauthMemberProfile(
            provider = OauthProvider.GOOGLE,
            providerId = claims.subject,
            email = email,
        )
    }

    private fun parseVerifiedClaims(idToken: String): JWTClaimsSet =
        parseIdTokenClaims(googleIdTokenProcessor, idToken).also { claims ->
            if (claims.issuer !in googleOauthProperties.issuers || googleOauthProperties.clientIds.none { it in claims.audience }) {
                throw OauthTokenInvalidException()
            }
        }
}
