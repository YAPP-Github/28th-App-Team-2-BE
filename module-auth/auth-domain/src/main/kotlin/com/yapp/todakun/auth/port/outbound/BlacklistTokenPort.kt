package com.yapp.todakun.auth.port.outbound

interface BlacklistTokenPort {
    fun blacklist(
        jti: String,
        remainingTtlSeconds: Long,
    )

    fun isBlacklisted(jti: String): Boolean
}
