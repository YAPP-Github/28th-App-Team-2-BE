package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.auth.claims.AccessTokenClaims
import com.yapp.todakun.auth.token.IssuedAccessToken
import java.util.UUID

interface AccessTokenPort {
    fun generate(memberId: UUID): IssuedAccessToken

    fun parse(token: String): AccessTokenClaims
}
