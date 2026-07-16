package com.yapp.todakun.auth.fixture

import com.nimbusds.jwt.JWTClaimsSet
import com.yapp.todakun.auth.adapter.oauth.google.GoogleOauthProperties

private const val ISSUER = "https://accounts.google.com"
private const val CLIENT_ID = "test-client-id"
private const val JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"

object GoogleOauthFixture {
    const val SUBJECT = "1234567890"
    const val EMAIL = "test@todakun.com"

    fun properties(
        clientIds: List<String> = listOf(CLIENT_ID),
        jwkSetUri: String = JWK_SET_URI,
        issuers: List<String> = listOf(ISSUER),
    ): GoogleOauthProperties =
        GoogleOauthProperties(
            clientIds = clientIds,
            jwkSetUri = jwkSetUri,
            issuers = issuers,
        )

    fun claims(
        subject: String = SUBJECT,
        issuer: String = ISSUER,
        audience: String = CLIENT_ID,
        email: String? = EMAIL,
        emailVerified: Any = true,
    ): JWTClaimsSet =
        JWTClaimsSet
            .Builder()
            .subject(subject)
            .issuer(issuer)
            .audience(audience)
            .claim("email", email)
            .claim("email_verified", emailVerified)
            .build()
}
