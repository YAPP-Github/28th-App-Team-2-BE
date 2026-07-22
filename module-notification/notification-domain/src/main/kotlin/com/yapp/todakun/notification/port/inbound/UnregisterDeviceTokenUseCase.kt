package com.yapp.todakun.notification.port.inbound

interface UnregisterDeviceTokenUseCase {
    fun unregister(token: String)
}
