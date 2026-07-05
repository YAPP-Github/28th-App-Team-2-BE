package com.yapp.todakun.auth.port

import com.yapp.todakun.auth.token.IssuedRefreshToken
import java.util.UUID

interface RefreshTokenPort {
    fun issue(memberId: UUID): IssuedRefreshToken

    fun findMemberId(token: String): UUID?

    fun revoke(token: String)

    fun revokeAll(memberId: UUID)
}
