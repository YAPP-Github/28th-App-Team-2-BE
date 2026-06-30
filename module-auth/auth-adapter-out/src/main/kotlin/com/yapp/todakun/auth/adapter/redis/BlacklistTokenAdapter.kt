package com.yapp.todakun.auth.adapter.redis

import com.yapp.todakun.auth.port.BlacklistTokenPort
import org.springframework.stereotype.Repository

@Repository
class BlacklistTokenAdapter(
    private val blacklistTokenRepository: BlacklistTokenRepository,
) : BlacklistTokenPort {
    override fun blacklist(
        jti: String,
        remainingTtlSeconds: Long,
    ) {
        blacklistTokenRepository.save(BlacklistToken(jti = jti, ttl = remainingTtlSeconds))
    }

    override fun isBlacklisted(jti: String): Boolean = blacklistTokenRepository.existsById(jti)
}
