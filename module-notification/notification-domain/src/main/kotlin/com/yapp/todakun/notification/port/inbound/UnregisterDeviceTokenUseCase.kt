package com.yapp.todakun.notification.port.inbound

import java.util.UUID

interface UnregisterDeviceTokenUseCase {
    fun unregister(
        memberId: UUID,
        token: String,
    )
}
