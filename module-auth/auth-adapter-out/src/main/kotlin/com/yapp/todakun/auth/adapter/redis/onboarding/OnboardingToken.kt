package com.yapp.todakun.auth.adapter.redis.onboarding

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive

@RedisHash("onboarding_token")
class OnboardingToken(
    @Id val value: String,
    val provider: String,
    val providerId: String,
    val email: String,
    @TimeToLive val ttl: Long,
)
