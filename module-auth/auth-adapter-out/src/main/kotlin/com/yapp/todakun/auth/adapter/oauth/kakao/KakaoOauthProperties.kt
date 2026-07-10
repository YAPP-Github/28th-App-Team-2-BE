package com.yapp.todakun.auth.adapter.oauth.kakao

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoOauthProperties(
    val userMeUri: String,
)
