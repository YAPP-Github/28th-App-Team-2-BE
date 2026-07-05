package com.yapp.todakun.auth.adapter.oauth.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.google")
data class GoogleOAuthProperties(
    val clientIds: List<String>,
    val jwkSetUri: String,
    val issuers: List<String>,
)
