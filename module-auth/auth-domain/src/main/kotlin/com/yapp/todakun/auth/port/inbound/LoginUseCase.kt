package com.yapp.todakun.auth.port.inbound

interface LoginUseCase {
    fun login(command: LoginCommand): LoginResult
}
