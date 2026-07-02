package com.yapp.todakun.auth.adapter.redis.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RefreshTokenProperties::class)
class RedisConfig
