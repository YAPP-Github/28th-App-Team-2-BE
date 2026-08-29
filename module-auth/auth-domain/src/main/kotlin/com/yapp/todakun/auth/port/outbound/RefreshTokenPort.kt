package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.auth.token.IssuedRefreshToken
import java.util.UUID

interface RefreshTokenPort {
    fun issue(memberId: UUID): IssuedRefreshToken

    fun consume(token: String): UUID?

    fun revokeAll(memberId: UUID)
}
