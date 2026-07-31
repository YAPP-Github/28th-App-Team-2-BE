package com.yapp.todakun.chat.adapter.redis

import com.yapp.todakun.chat.exception.ChatDailyQuotaExceededException
import com.yapp.todakun.chat.port.outbound.ChatQuotaPort
import com.yapp.todakun.chat.port.outbound.ChatQuotaStatus
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val QUOTA_KEY_PREFIX = "chat:quota:"
private const val DAILY_FREE_CHAT_LIMIT = 3
private val ZONE = ZoneId.of("Asia/Seoul")
private val DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE

/** 회원별 하루 무료 채팅 횟수를 Redis 카운터(자정 만료 TTL)로 관리하는 [ChatQuotaPort] 구현체. */
@Component
class RedisChatQuotaAdapter(
    private val redisTemplate: StringRedisTemplate,
) : ChatQuotaPort {
    override fun reserve(memberId: UUID): ChatQuotaStatus {
        val key = keyOf(memberId)
        // INCR은 키가 없으면 0에서 시작해 원자적으로 1 증가시키므로, 존재 확인 후 증가시키는 방식의 경쟁 조건이 없다.
        val used = redisTemplate.opsForValue().increment(key) ?: 1L
        if (used == 1L) {
            redisTemplate.expire(key, ttlUntilMidnight())
        }
        if (used > DAILY_FREE_CHAT_LIMIT) {
            redisTemplate.opsForValue().decrement(key)
            throw ChatDailyQuotaExceededException()
        }

        return ChatQuotaStatus(used = used.toInt(), limit = DAILY_FREE_CHAT_LIMIT)
    }

    override fun refund(memberId: UUID) {
        val key = keyOf(memberId)
        val remaining = redisTemplate.opsForValue().decrement(key) ?: 0L
        if (remaining <= 0) redisTemplate.delete(key)
    }

    override fun getStatus(memberId: UUID): ChatQuotaStatus {
        val used = redisTemplate.opsForValue().get(keyOf(memberId))?.toIntOrNull() ?: 0

        return ChatQuotaStatus(used = used, limit = DAILY_FREE_CHAT_LIMIT)
    }

    private fun keyOf(memberId: UUID): String = "$QUOTA_KEY_PREFIX$memberId:${LocalDate.now(ZONE).format(DATE_KEY_FORMATTER)}"

    private fun ttlUntilMidnight(): Duration {
        val nextMidnight = LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toInstant()

        return Duration.between(Instant.now(), nextMidnight)
    }
}
