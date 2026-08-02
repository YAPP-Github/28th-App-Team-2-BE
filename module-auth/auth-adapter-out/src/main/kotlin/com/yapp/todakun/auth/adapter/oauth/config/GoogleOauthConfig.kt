package com.yapp.todakun.auth.adapter.oauth.config

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.adapter.oauth.createRs256JwtProcessor
import com.yapp.todakun.auth.adapter.oauth.google.GoogleOauthProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(GoogleOauthProperties::class)
class GoogleOauthConfig(
    private val googleOauthProperties: GoogleOauthProperties,
) {
    @Bean
    fun googleIdTokenProcessor(): ConfigurableJWTProcessor<SecurityContext> = createRs256JwtProcessor(googleOauthProperties.jwkSetUri)
}
