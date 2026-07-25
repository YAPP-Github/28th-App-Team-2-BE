package com.yapp.todakun.auth.port.inbound

interface RefreshUseCase {
    fun refresh(command: RefreshCommand): RefreshResult
}
