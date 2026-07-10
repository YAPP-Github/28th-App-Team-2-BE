package com.yapp.todakun.auth.adapter.jwt.access

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "token.jwt")
data class AccessTokenProperties(
    val secret: String,
    val expirySeconds: Long,
)
