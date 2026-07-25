package com.yapp.todakun.auth.adapter.redis.refresh

import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.auth.token.IssuedRefreshToken
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val CONSUME_LOCK_TTL = Duration.ofSeconds(5)
private const val CONSUME_LOCK_KEY_PREFIX = "refresh_token:consume:"

@Component
class RefreshTokenAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenProperties: RefreshTokenProperties,
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenPort {
    @ExperimentalUuidApi
    override fun issue(memberId: UUID): IssuedRefreshToken {
        val token = Uuid.generateV7().toJavaUuid().toString()
        refreshTokenRepository.save(
            RefreshToken(
                value = token,
                memberId = memberId.toString(),
                ttl = refreshTokenProperties.expirySeconds,
            ),
        )

        return IssuedRefreshToken(value = token, expiresInSeconds = refreshTokenProperties.expirySeconds)
    }

    // 동일 토큰의 동시 재사용을 막기 위해 SET NX로 소비 권한을 선점한 요청만 조회·폐기를 진행한다.
    override fun consume(token: String): UUID? {
        val acquired = redisTemplate.opsForValue().setIfAbsent("$CONSUME_LOCK_KEY_PREFIX$token", "1", CONSUME_LOCK_TTL)
        if (acquired != true) return null

        val memberId = refreshTokenRepository.findById(token).getOrNull()?.memberId?.let(UUID::fromString) ?: return null
        refreshTokenRepository.deleteById(token)

        return memberId
    }

    // Spring Data Redis는 파생 삭제 쿼리(deleteAllByX)를 지원하지 않아 조회 후 삭제한다.
    override fun revokeAll(memberId: UUID) {
        val tokens = refreshTokenRepository.findAllByMemberId(memberId.toString())

        refreshTokenRepository.deleteAll(tokens)
    }
}
