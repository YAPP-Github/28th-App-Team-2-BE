package com.yapp.todakun.notification.adapter.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class UnregisterDeviceTokenRequest(
    @field:Schema(description = "해제할 FCM 등록 토큰")
    @field:NotBlank(message = "토큰을 입력해 주세요.")
    val token: String,
)
