package com.yapp.todakun.auth.adapter.oauth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import java.net.URI

/** Google/Apple이 동일하게 쓰는 RS256 JWKS 기반 id_token 검증용 JWTProcessor 생성. */
internal fun createRs256JwtProcessor(jwkSetUri: String): ConfigurableJWTProcessor<SecurityContext> {
    val jwkSetUrl = URI(jwkSetUri).toURL()
    val jwkSource = JWKSourceBuilder.create<SecurityContext>(jwkSetUrl).build()
    val keySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)

    return DefaultJWTProcessor<SecurityContext>().apply {
        jwsKeySelector = keySelector
        jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(null, null)
    }
}
