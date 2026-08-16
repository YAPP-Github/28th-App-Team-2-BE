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
    @Operation(
        summary = "공지 발송",
        description =
            "전체 회원에게 NOTICE 타입 알림을 fan-out 발송한다. type으로 공지의 세부 분류(GENERAL/EVENT/MAINTENANCE/UPDATE)를 지정한다. " +
                "ROLE_ADMIN 권한이 있어야 호출할 수 있다. " +
                "같은 요청을 반복해도 실제 발송은 최초 1회만 일어난다(영속 멱등성). 중복 판단 기준은 하나뿐이다 — " +
                "idempotencyKey를 지정하면 그 키만 기준이 되고(제목·본문·딥링크는 보지 않는다), " +
                "지정하지 않으면 제목·본문·딥링크에서 파생한 키가 기준이 된다. " +
                "따라서 내용이 완전히 같아도 새 idempotencyKey를 주면 의도적으로 재발송할 수 있고, " +
                "반대로 내용이 달라도 같은 idempotencyKey를 주면 두 번째 요청은 발송되지 않는다.",
    )
    @PostMapping("api/v1/admin/notifications/notice")
    fun publishNotice(
        @RequestBody @Valid request: PublishNoticeRequest,
    ): ResponseEntity<CommonResponse<Unit>>
}
