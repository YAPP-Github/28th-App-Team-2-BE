package com.yapp.todakun.notification.adapter.web

import com.yapp.todakun.notification.adapter.web.dto.request.PublishNoticeRequest
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "AdminNotice", description = "관리자 공지 발송 API(ROLE_ADMIN 전용)")
interface AdminNoticeApi {
    @Operation(summary = "공지 발송", description = "전체 회원에게 NOTICE 타입 알림을 fan-out 발송한다. ROLE_ADMIN 권한이 있어야 호출할 수 있다.")
    @PostMapping("api/v1/admin/notifications/notice")
    fun publishNotice(
        @RequestBody @Valid request: PublishNoticeRequest,
    ): ResponseEntity<CommonResponse<Unit>>
}
