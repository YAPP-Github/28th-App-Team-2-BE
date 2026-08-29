package com.yapp.todakun.auth.port.inbound

data class LogoutCommand(
    val accessToken: String,
)
