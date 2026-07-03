package com.yapp.todakun.auth.adapter.redis.config

import com.yapp.todakun.auth.adapter.redis.properties.RefreshTokenProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RefreshTokenProperties::class)
class RedisConfig
