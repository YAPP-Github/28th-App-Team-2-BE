package com.yapp.todakun.auth.adapter.jwt.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AccessTokenProperties::class)
class JwtConfig
