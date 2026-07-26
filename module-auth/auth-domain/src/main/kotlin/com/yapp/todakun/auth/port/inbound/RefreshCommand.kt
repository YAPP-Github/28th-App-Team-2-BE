package com.yapp.todakun.auth.port.inbound

data class RefreshCommand(
    val refreshToken: String,
)
