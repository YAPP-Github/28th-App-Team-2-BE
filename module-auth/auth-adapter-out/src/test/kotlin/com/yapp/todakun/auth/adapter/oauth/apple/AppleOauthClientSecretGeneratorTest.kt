package com.yapp.todakun.auth.adapter.oauth.apple

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.SignedJWT
import com.yapp.todakun.auth.fixture.AppleOauthFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.Date

class AppleOauthClientSecretGeneratorTest :
    DescribeSpec({
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val privateKeyBase64 = Base64.getEncoder().encodeToString((keyPair.private as ECPrivateKey).encoded)
        val properties = AppleOauthFixture.properties(privateKey = privateKeyBase64)
        val generator = AppleOauthClientSecretGenerator(properties)

        describe("generate") {
            context("client_secret을 생성하면") {
                it("ES256으로 서명되고 Apple이 요구하는 클레임을 담은 JWT를 반환한다") {
                    val clientSecret = generator.generate(AppleOauthFixture.CLIENT_ID)

                    val signedJwt = SignedJWT.parse(clientSecret)
                    signedJwt.verify(ECDSAVerifier(keyPair.public as ECPublicKey)).shouldBeTrue()
                    signedJwt.header.algorithm shouldBe JWSAlgorithm.ES256
                    signedJwt.header.keyID shouldBe properties.keyId
                    signedJwt.jwtClaimsSet.issuer shouldBe properties.teamId
                    signedJwt.jwtClaimsSet.subject shouldBe AppleOauthFixture.CLIENT_ID
                    signedJwt.jwtClaimsSet.audience shouldBe listOf(properties.issuer)
                    signedJwt.jwtClaimsSet.expirationTime.after(Date()).shouldBeTrue()
                }
            }
        }
    })
