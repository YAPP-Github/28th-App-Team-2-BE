package com.yapp.todakun.auth.adapter.redis.refresh

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "token.redis")
data class RefreshTokenProperties(
    val expirySeconds: Long,
)
