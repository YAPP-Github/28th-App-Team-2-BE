package com.yapp.todakun.auth.fixture

import com.nimbusds.jwt.JWTClaimsSet
import com.yapp.todakun.auth.AppleOauthCredential
import com.yapp.todakun.auth.adapter.oauth.apple.AppleOauthProperties
import java.util.UUID

private const val ISSUER = "https://appleid.apple.com"
private const val JWK_SET_URI = "https://appleid.apple.com/auth/keys"
private const val TEAM_ID = "TEST_TEAM_ID"
private const val KEY_ID = "TEST_KEY_ID"
private const val PRIVATE_KEY = "test-private-key-base64"
private const val TOKEN_ENDPOINT = "https://appleid.apple.com/auth/token"
private const val REVOKE_ENDPOINT = "https://appleid.apple.com/auth/revoke"

// credential()에서만 쓰는 기본 식별자 — properties()용 상수들과는 목적이 달라 구분해 둔다.
private val CREDENTIAL_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000003")

object AppleOauthFixture {
    const val SUBJECT = "apple-provider-id"
    const val EMAIL = "test@todakun.com"
    const val CLIENT_ID = "com.yapp.todakun"
    const val REFRESH_TOKEN = "apple-refresh-token"

    fun credential(
        id: UUID = CREDENTIAL_ID,
        providerId: String = SUBJECT,
        clientId: String = CLIENT_ID,
        refreshToken: String = REFRESH_TOKEN,
    ): AppleOauthCredential = AppleOauthCredential.reconstitute(id, providerId, clientId, refreshToken)

    fun properties(
        clientIds: List<String> = listOf(CLIENT_ID),
        jwkSetUri: String = JWK_SET_URI,
        issuer: String = ISSUER,
        teamId: String = TEAM_ID,
        keyId: String = KEY_ID,
        privateKey: String = PRIVATE_KEY,
        tokenEndpoint: String = TOKEN_ENDPOINT,
        revokeEndpoint: String = REVOKE_ENDPOINT,
    ): AppleOauthProperties =
        AppleOauthProperties(
            clientIds = clientIds,
            jwkSetUri = jwkSetUri,
            issuer = issuer,
            teamId = teamId,
            keyId = keyId,
            privateKey = privateKey,
            tokenEndpoint = tokenEndpoint,
            revokeEndpoint = revokeEndpoint,
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
