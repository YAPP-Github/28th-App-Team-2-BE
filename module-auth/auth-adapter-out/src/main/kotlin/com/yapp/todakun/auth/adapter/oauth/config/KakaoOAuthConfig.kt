package com.yapp.todakun.auth.adapter.oauth.config

import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoOAuthProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(KakaoOAuthProperties::class)
class KakaoOAuthConfig {
    @Bean
    fun restClient(): RestClient = RestClient.create()
}
