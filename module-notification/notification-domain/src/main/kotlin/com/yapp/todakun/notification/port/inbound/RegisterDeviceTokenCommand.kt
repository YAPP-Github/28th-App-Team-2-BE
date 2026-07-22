package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.Platform
import java.util.UUID

data class RegisterDeviceTokenCommand(
    val memberId: UUID,
    val token: String,
    val platform: Platform,
)
