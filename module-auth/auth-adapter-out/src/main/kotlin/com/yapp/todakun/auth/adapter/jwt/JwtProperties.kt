package com.yapp.todakun.auth.adapter.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpirySeconds: Long,
    val refreshTokenExpirySeconds: Long,
)
