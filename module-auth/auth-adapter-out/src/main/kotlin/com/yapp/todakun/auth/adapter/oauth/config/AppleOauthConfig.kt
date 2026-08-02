package com.yapp.todakun.auth.adapter.oauth.config

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.adapter.oauth.apple.AppleOauthProperties
import com.yapp.todakun.auth.adapter.oauth.createRs256JwtProcessor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AppleOauthProperties::class)
class AppleOauthConfig(
    private val appleOauthProperties: AppleOauthProperties,
) {
    @Bean
    fun appleIdTokenProcessor(): ConfigurableJWTProcessor<SecurityContext> = createRs256JwtProcessor(appleOauthProperties.jwkSetUri)
}
