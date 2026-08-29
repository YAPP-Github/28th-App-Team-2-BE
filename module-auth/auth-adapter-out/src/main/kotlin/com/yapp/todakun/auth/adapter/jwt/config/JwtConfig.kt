package com.yapp.todakun.auth.adapter.jwt.config

import com.yapp.todakun.auth.adapter.jwt.access.AccessTokenProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AccessTokenProperties::class)
class JwtConfig
