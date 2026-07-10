package com.yapp.todakun.auth.adapter.oauth.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.yapp.todakun.auth.adapter.oauth.google.GoogleOauthProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@EnableConfigurationProperties(GoogleOauthProperties::class)
class GoogleOauthConfig(
    private val googleOauthProperties: GoogleOauthProperties,
) {
    @Bean
    fun googleIdTokenProcessor(): ConfigurableJWTProcessor<SecurityContext> {
        val jwkSetUrl = URI(googleOauthProperties.jwkSetUri).toURL()
        val jwkSource = JWKSourceBuilder.create<SecurityContext>(jwkSetUrl).build()
        val keySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)

        return DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = keySelector
            jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(null, null)
        }
    }
}
