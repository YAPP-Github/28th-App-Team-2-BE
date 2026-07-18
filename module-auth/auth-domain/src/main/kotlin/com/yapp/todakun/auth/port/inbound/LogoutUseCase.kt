package com.yapp.todakun.auth.port.inbound

interface LogoutUseCase {
    fun logout(command: LogoutCommand)
}
