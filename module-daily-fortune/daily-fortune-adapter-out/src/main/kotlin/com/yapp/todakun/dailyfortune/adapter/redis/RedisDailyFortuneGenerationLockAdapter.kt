package com.yapp.todakun.dailyfortune.adapter.redis

import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneGenerationLockPort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

private const val LOCK_KEY_PREFIX = "daily-fortune:generating:"
private val LOCK_TTL: Duration = Duration.ofSeconds(150)

/**
 * (memberId, fortuneDate) 조합의 AI 생성 진행 여부를 Redis SETNX(+TTL)로 표시하는 [DailyFortuneGenerationLockPort] 구현체.
 * TTL은 AI 최대 지연(TimeLimiter 120초)보다 넉넉하게 잡아, release 없이 워커가 죽어도 영구히 잠기지 않게 하는 안전망이다.
 */
@Component
class RedisDailyFortuneGenerationLockAdapter(
    private val redisTemplate: StringRedisTemplate,
) : DailyFortuneGenerationLockPort {
    override fun tryAcquire(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): Boolean = redisTemplate.opsForValue().setIfAbsent(keyOf(memberId, fortuneDate), "1", LOCK_TTL) ?: false

    override fun release(
        memberId: UUID,
        fortuneDate: LocalDate,
    ) {
        redisTemplate.delete(keyOf(memberId, fortuneDate))
    }

    private fun keyOf(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): String = "$LOCK_KEY_PREFIX$memberId:$fortuneDate"
}
