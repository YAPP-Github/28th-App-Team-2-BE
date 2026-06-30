package com.yapp.todakun.auth.adapter.redis

import com.yapp.todakun.auth.port.RefreshTokenPort
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RefreshTokenAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
) : RefreshTokenPort {
    override fun save(
        jti: String,
        memberId: UUID,
        ttlSeconds: Long,
    ) {
        refreshTokenRepository.save(RefreshToken(jti = jti, memberId = memberId.toString(), ttl = ttlSeconds))
    }

    override fun existsByJti(jti: String): Boolean = refreshTokenRepository.existsById(jti)

    override fun deleteByJti(jti: String) {
        refreshTokenRepository.deleteById(jti)
    }

    override fun deleteAllByMemberId(memberId: UUID) {
        refreshTokenRepository.deleteAllByMemberId(memberId.toString())
    }
}
