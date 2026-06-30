package com.yapp.todakun.auth.port

import java.util.UUID

interface RefreshTokenPort {
    fun save(
        jti: String,
        memberId: UUID,
        ttlSeconds: Long,
    )

    fun existsByJti(jti: String): Boolean

    fun deleteByJti(jti: String)

    fun deleteAllByMemberId(memberId: UUID)
}
