package com.yapp.todakun.auth.adapter.oauth.config

import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoOauthProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(KakaoOauthProperties::class)
class KakaoOauthConfig {
    @Bean
    fun restClient(): RestClient =
        RestClient.builder()
            .requestFactory(createRequestFactory())
            .build()

    private fun createRequestFactory() =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(3))
            setReadTimeout(Duration.ofSeconds(5))
        }
}
