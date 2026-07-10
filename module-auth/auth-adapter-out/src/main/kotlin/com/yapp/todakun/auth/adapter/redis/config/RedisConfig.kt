package com.yapp.todakun.auth.adapter.redis.config

import com.yapp.todakun.auth.adapter.redis.onboarding.OnboardingTokenProperties
import com.yapp.todakun.auth.adapter.redis.refresh.RefreshTokenProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RefreshTokenProperties::class, OnboardingTokenProperties::class)
class RedisConfig
