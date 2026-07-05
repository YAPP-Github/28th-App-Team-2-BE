package com.yapp.todakun.auth.adapter.redis

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed

@RedisHash("refresh_token")
class RefreshToken(
    @Id val value: String,
    @Indexed val memberId: String,
    @TimeToLive val ttl: Long,
)
