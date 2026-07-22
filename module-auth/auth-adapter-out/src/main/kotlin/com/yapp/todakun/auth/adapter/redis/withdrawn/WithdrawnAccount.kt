package com.yapp.todakun.auth.adapter.redis.withdrawn

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive

/**
 * 재가입 제한 대상 SNS 식별자. [id]는 `provider:providerId`의 SHA-256 해시(비식별)이며,
 * [ttl]초(정책상 90일) 후 Redis가 자동 만료시켜 재가입 제한이 해제된다(별도 배치·스케줄러 불필요).
 */
@RedisHash("withdrawn_account")
class WithdrawnAccount(
    @Id val id: String,
    @TimeToLive val ttl: Long,
)
