package com.yapp.todakun.auth.port

import com.yapp.todakun.auth.AccessTokenClaims
import com.yapp.todakun.auth.IssuedAccessToken
import java.util.UUID

interface AccessTokenPort {
    fun generate(memberId: UUID): IssuedAccessToken

    fun parse(token: String): AccessTokenClaims
}
