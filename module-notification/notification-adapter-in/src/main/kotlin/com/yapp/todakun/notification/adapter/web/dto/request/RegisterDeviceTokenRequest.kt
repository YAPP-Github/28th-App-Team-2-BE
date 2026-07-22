package com.yapp.todakun.notification.adapter.web.dto.request

import com.yapp.todakun.notification.Platform
import com.yapp.todakun.notification.port.inbound.RegisterDeviceTokenCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class RegisterDeviceTokenRequest(
    @field:Schema(description = "FCM 등록 토큰")
    @field:NotBlank(message = "토큰을 입력해 주세요.")
    val token: String,
    @field:Schema(description = "디바이스 플랫폼", example = "IOS")
    val platform: Platform,
) {
    fun toCommand(memberId: UUID) = RegisterDeviceTokenCommand(memberId = memberId, token = token, platform = platform)
}
