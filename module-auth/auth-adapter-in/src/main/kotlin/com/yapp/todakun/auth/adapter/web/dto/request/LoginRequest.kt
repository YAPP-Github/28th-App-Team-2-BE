package com.yapp.todakun.auth.adapter.web.dto.request

import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.shared.OauthProvider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class LoginRequest(
    @field:NotNull(message = "OauthProvider를 입력해 주세요.")
    val provider: OauthProvider,
    @field:NotBlank(message = "OAuth 액세스 토큰을 입력해 주세요.")
    val oauthAccessToken: String,
) {
    fun toCommand() =
        LoginCommand(
            provider = provider,
            oauthAccessToken = oauthAccessToken,
        )
}
