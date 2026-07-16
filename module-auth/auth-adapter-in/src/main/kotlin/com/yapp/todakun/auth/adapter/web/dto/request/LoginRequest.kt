package com.yapp.todakun.auth.adapter.web.dto.request

import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.shared.OauthProvider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class LoginRequest(
    @field:NotNull
    val provider: OauthProvider,
    @field:NotBlank
    val oauthAccessToken: String,
) {
    fun toCommand() =
        LoginCommand(
            provider = provider,
            oauthAccessToken = oauthAccessToken,
        )
}
