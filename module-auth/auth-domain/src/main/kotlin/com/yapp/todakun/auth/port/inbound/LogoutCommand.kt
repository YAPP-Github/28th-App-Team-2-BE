package com.yapp.todakun.auth.port.inbound

import java.util.UUID

data class LogoutCommand(
    val memberId: UUID,
    val jti: String,
    val remainingSeconds: Long,
)
