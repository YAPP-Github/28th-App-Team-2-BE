package com.yapp.todakun.notification.adapter.web

import com.yapp.todakun.notification.adapter.web.dto.request.UpdateNotificationSettingRequest
import com.yapp.todakun.notification.adapter.web.dto.response.NotificationSettingResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "NotificationSetting", description = "알림 설정 API")
interface NotificationSettingApi {
    @Operation(
        summary = "알림 설정 조회",
        description = "인증된 회원의 알림 설정(아침 운 리포트/토닥이/행운 액션 토글 + 받을 시간)을 조회한다. 미설정 회원은 기본값(전부 OFF)을 반환한다.",
    )
    @GetMapping("api/v1/notifications/settings")
    fun getSetting(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<NotificationSettingResponse>>

    @Operation(summary = "알림 설정 변경", description = "알림 토글 3종과 아침 운 리포트 받을 시간을 저장한다(받을 시간은 30분 단위로 스냅).")
    @PatchMapping("api/v1/notifications/settings")
    fun updateSetting(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @RequestBody @Valid request: UpdateNotificationSettingRequest,
    ): ResponseEntity<CommonResponse<NotificationSettingResponse>>
}
