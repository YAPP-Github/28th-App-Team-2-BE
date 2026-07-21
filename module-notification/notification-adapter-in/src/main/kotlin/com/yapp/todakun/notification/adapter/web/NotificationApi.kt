package com.yapp.todakun.notification.adapter.web

import com.yapp.todakun.notification.adapter.web.dto.response.NotificationListResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "Notification", description = "알림함 API")
interface NotificationApi {
    @Operation(
        summary = "알림 목록 조회",
        description = "인증된 회원의 인앱 알림 목록(최신순)과 안읽음 개수를 조회한다. 홈 상단 배너·안읽음 점 표시에 사용한다.",
    )
    @GetMapping("api/v1/notifications")
    fun getNotifications(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<NotificationListResponse>>

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경한다. 본인의 알림만 처리할 수 있다.")
    @PatchMapping("api/v1/notifications/{notificationId}/read")
    fun readNotification(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @Parameter(description = "알림 ID")
        @PathVariable notificationId: UUID,
    ): ResponseEntity<CommonResponse<Unit>>

    @Operation(summary = "알림 전체 읽음 처리", description = "회원의 모든 알림을 읽음 상태로 변경한다.")
    @PatchMapping("api/v1/notifications/read-all")
    fun readAllNotifications(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<Unit>>
}
