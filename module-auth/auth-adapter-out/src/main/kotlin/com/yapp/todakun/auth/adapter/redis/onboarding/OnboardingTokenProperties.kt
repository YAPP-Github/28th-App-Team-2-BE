package com.yapp.todakun.auth.adapter.redis.onboarding

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "token.onboarding")
data class OnboardingTokenProperties(
    val expirySeconds: Long,
)
