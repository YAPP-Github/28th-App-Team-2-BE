package com.yapp.todakun.auth.adapter.oauth.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoOAuthProperties(
    val userMeUri: String,
)
