package com.yapp.todakun.auth.adapter.oauth.google

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.google")
data class GoogleOauthProperties(
    val clientIds: List<String>,
    val jwkSetUri: String,
    val issuers: List<String>,
)
