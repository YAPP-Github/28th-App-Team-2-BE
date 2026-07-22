package com.yapp.todakun.notification.port.inbound

interface RegisterDeviceTokenUseCase {
    fun register(command: RegisterDeviceTokenCommand)
}
