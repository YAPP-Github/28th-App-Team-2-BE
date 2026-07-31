package com.yapp.todakun.auth.adapter.oauth.apple

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date

private const val EXPIRATION_SECONDS = 300L

/**
 * Apple 토큰 교환/철회 요청에 매번 필요한 client_secret(ES256 서명 JWT)을 생성한다.
 * 만료 시간이 짧고(5분) 호출 즉시 사용되므로 캐싱하지 않고 매 호출마다 새로 생성한다.
 */
@Component
class AppleClientSecretGenerator(
    private val appleOauthProperties: AppleOauthProperties,
) {
    fun generate(clientId: String): String {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256).keyID(appleOauthProperties.keyId).build()

        return SignedJWT(header, buildClaims(clientId)).apply { sign(ECDSASigner(parsePrivateKey())) }.serialize()
    }

    private fun buildClaims(clientId: String): JWTClaimsSet {
        val now = Instant.now()

        return JWTClaimsSet.Builder()
            .issuer(appleOauthProperties.teamId)
            .subject(clientId)
            .audience(appleOauthProperties.issuer)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
            .build()
    }

    private fun parsePrivateKey(): ECPrivateKey {
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(appleOauthProperties.privateKey))

        return KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
    }
}
