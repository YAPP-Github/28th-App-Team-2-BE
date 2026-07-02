package com.yapp.todakun.auth.adapter.jwt.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "token.jwt")
data class AccessTokenProperties(
    val secret: String,
    val expirySeconds: Long,
)
