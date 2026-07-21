package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.Platform
import java.util.UUID

interface RegisterDeviceTokenUseCase {
    fun register(command: RegisterDeviceTokenCommand)
}

interface UnregisterDeviceTokenUseCase {
    fun unregister(token: String)
}

data class RegisterDeviceTokenCommand(
    val memberId: UUID,
    val token: String,
    val platform: Platform,
)
