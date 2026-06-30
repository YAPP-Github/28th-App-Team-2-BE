package com.yapp.todakun.auth.adapter.redis

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive

@RedisHash("blacklist_token")
class BlacklistToken(
    @Id val jti: String,
    @TimeToLive val ttl: Long, 
)
