package com.yapp.todakun.auth

import java.util.UUID

data class AccessTokenClaims(
    val memberId: UUID,
    val jti: String,
    val remainingSeconds: Long,
)
