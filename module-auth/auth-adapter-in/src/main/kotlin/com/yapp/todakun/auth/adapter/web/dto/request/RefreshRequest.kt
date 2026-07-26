package com.yapp.todakun.auth.adapter.web.dto.request

import com.yapp.todakun.auth.port.inbound.RefreshCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:Schema(description = "Refresh 토큰", example = "01930000-0000-7000-8000-000000000000")
    @field:NotBlank(message = "Refresh 토큰을 입력해 주세요.")
    val refreshToken: String,
) {
    fun toCommand() = RefreshCommand(refreshToken = refreshToken)
}
