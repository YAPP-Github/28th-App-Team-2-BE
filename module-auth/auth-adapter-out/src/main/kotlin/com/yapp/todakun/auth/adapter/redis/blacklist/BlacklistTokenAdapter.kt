package com.yapp.todakun.auth.adapter.redis.blacklist

import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import org.springframework.stereotype.Component

@Component
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
