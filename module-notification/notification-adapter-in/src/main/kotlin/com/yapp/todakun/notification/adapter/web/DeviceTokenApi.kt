package com.yapp.todakun.notification.adapter.web

import com.yapp.todakun.notification.adapter.web.dto.request.RegisterDeviceTokenRequest
import com.yapp.todakun.notification.adapter.web.dto.request.UnregisterDeviceTokenRequest
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "DeviceToken", description = "디바이스 토큰 API")
interface DeviceTokenApi {
    @Operation(
        summary = "디바이스 토큰 등록",
        description = "클라이언트가 발급받은 FCM 등록 토큰을 저장한다. 같은 토큰이 있으면 소유 회원만 갱신한다(upsert).",
    )
    @PostMapping("api/v1/notifications/device-tokens")
    fun register(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @RequestBody @Valid request: RegisterDeviceTokenRequest,
    ): ResponseEntity<CommonResponse<Unit>>

    @Operation(summary = "디바이스 토큰 해제", description = "로그아웃 시 해당 기기의 FCM 토큰을 제거한다.")
    @DeleteMapping("api/v1/notifications/device-tokens")
    fun unregister(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @RequestBody @Valid request: UnregisterDeviceTokenRequest,
    ): ResponseEntity<CommonResponse<Unit>>
}
