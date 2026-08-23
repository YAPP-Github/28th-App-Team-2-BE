package com.yapp.todakun.dailyfortune.adapter.redis

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneGenerationLockPort
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val LOCK_KEY_PREFIX = "daily-fortune:generating:"

// daily-fortune-ai TimeLimiter(application.yaml `ai-resilience.time-limiters.daily-fortune-ai.timeout-duration-seconds`,
// 현재 120초)보다 반드시 커야 한다 — 짧아지면 아직 생성 중인 호출의 락이 만료돼 다른 호출자가 가로챌 수 있다.
private val LOCK_TTL: Duration = Duration.ofSeconds(150)

// 토큰이 일치할 때만 삭제한다(compare-and-delete). TTL 만료 후 다른 호출자가 이미 새로 선점한 락을
// 원래 호출자의 뒤늦은 release()가 잘못 지우는 걸 막는다(단순 DEL은 이 경합을 막지 못한다).
private val RELEASE_IF_OWNER_SCRIPT =
    DefaultRedisScript(
        """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """.trimIndent(),
        Long::class.java,
    )

/**
 * (memberId, fortuneDate) 조합의 AI 생성 진행 여부를 Redis SETNX(+TTL)로 표시하는 [DailyFortuneGenerationLockPort] 구현체.
 * 선점마다 고유 토큰을 값으로 저장하고, 해제 시 그 토큰을 [release]로 넘겨받아 소유권이 일치할 때만 삭제한다.
 *
 * Redis 장애로 락 자체를 걸거나 풀 수 없어도 오늘의 운세 생성(핵심 기능)을 막으면 안 된다(fail-open) —
 * 중복 생성 방지는 최적화일 뿐이라, 이 우선순위가 뒤바뀌면 Redis 장애 하나로 배치 전체·자가 치유가 멈춘다.
 * `FailOpenCacheErrorHandler`(이슈 #56)와 동일한 원칙이다. tryAcquire 실패 시 "선점 성공"으로 간주해
 * 그대로 생성을 진행시키고, release 실패는 TTL이 지나면 자연히 풀리므로 로그만 남기고 흡수한다.
 */
@Component
@Loggable
class RedisDailyFortuneGenerationLockAdapter(
    private val redisTemplate: StringRedisTemplate,
) : DailyFortuneGenerationLockPort {
    @ExperimentalUuidApi
    override fun tryAcquire(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): String? {
        val token = Uuid.generateV7().toJavaUuid().toString()

        return try {
            val acquired = redisTemplate.opsForValue().setIfAbsent(keyOf(memberId, fortuneDate), token, LOCK_TTL) ?: false
            if (acquired) token else null
        } catch (e: DataAccessException) {
            log.error("생성 락 선점 실패(Redis 데이터 액세스 장애) - 락 없이 생성 진행: fortuneDate={}", fortuneDate, e)
            token
        }
    }

    override fun release(
        memberId: UUID,
        fortuneDate: LocalDate,
        token: String,
    ) {
        try {
            redisTemplate.execute(RELEASE_IF_OWNER_SCRIPT, listOf(keyOf(memberId, fortuneDate)), token)
        } catch (e: DataAccessException) {
            log.error("생성 락 해제 실패(Redis 데이터 액세스 장애) - TTL 만료까지 자연 해제되지 않음: fortuneDate={}", fortuneDate, e)
        }
    }

    private fun keyOf(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): String = "$LOCK_KEY_PREFIX$memberId:$fortuneDate"
}
