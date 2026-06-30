package com.yapp.todakun.auth.adapter.redis

import com.yapp.todakun.auth.IssuedRefreshToken
import com.yapp.todakun.auth.adapter.jwt.JwtProperties
import com.yapp.todakun.auth.port.RefreshTokenPort
import org.springframework.stereotype.Repository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@Repository
class RefreshTokenAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
) : RefreshTokenPort {
    @OptIn(ExperimentalUuidApi::class)
    override fun issue(memberId: UUID): IssuedRefreshToken {
        val token = Uuid.generateV7().toJavaUuid().toString()
        refreshTokenRepository.save(
            RefreshToken(
                jti = token,
                memberId = memberId.toString(),
                ttl = jwtProperties.refreshTokenExpirySeconds,
            ),
        )
        return IssuedRefreshToken(value = token, expiresInSeconds = jwtProperties.refreshTokenExpirySeconds)
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
