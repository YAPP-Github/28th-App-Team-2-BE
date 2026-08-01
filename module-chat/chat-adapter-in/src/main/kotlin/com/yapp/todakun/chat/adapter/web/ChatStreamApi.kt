package com.yapp.todakun.chat.adapter.web

import com.yapp.todakun.chat.adapter.web.dto.request.SendChatMessageRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@Tag(name = "ChatStream", description = "토닥이 답변 스트리밍 API(SSE)")
interface ChatStreamApi {
    @Operation(
        summary = "토닥이에게 메시지 전송(SSE 스트리밍)",
        description =
            "메시지를 보내고 답변을 SSE로 스트리밍 수신한다. 이벤트 순서: start → delta(반복) → (action) → done, " +
                "실패 시 error. SSE 연결이 끊겨도 답변 생성·저장·알림 발송은 서버에서 끝까지 진행된다.",
    )
    @PostMapping("api/v1/chat/messages", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun sendMessage(
        @Valid @RequestBody request: SendChatMessageRequest,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): SseEmitter
}
