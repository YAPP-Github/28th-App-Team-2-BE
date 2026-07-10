package com.yapp.todakun.auth.adapter.redis.refresh

import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.auth.token.IssuedRefreshToken
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@Component
class RefreshTokenAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenProperties: RefreshTokenProperties,
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

    override fun findMemberId(token: String): UUID? =
        refreshTokenRepository.findById(token)
            .map { UUID.fromString(it.memberId) }
            .orElse(null)

    override fun revoke(token: String) {
        refreshTokenRepository.deleteById(token)
    }

    override fun revokeAll(memberId: UUID) {
        refreshTokenRepository.deleteAllByMemberId(memberId.toString())
    }
}
