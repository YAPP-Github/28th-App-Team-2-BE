package com.yapp.todakun.auth.adapter.web.dto.request

import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.shared.OauthProvider
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class LoginRequest(
    @field:Schema(description = "OAuth 제공자", example = "KAKAO")
    @field:NotNull(message = "OAuth 제공자를 입력해 주세요.")
    val provider: OauthProvider,
    @field:Schema(description = "OAuth 액세스 토큰(Apple은 ID 토큰)", example = "eyJhbGciOiJIUzI1NiJ9...")
    @field:NotBlank(message = "OAuth 액세스 토큰을 입력해 주세요.")
    val oauthAccessToken: String,
    @field:Schema(description = "Apple 최초 로그인 시 함께 전달되는 authorization code(Kakao/Google은 불필요)", example = "c1234.abcd...")
    val authorizationCode: String? = null,
) {
    fun toCommand() =
        LoginCommand(
            provider = provider,
            oauthAccessToken = oauthAccessToken,
            authorizationCode = authorizationCode,
        )
}
