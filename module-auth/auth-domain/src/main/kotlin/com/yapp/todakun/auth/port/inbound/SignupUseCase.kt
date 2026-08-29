package com.yapp.todakun.auth.port.inbound

interface SignupUseCase {
    fun signup(command: SignupCommand): SignupResult
}
