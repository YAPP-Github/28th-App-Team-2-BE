package com.yapp.todakun.chat.adapter.web.dto.request

import com.yapp.todakun.chat.port.inbound.SendChatMessageCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/** [conversationId]가 null이면 새 대화("새 채팅")를 시작하고, 있으면 기존 대화를 이어간다. */
data class SendChatMessageRequest(
    val conversationId: UUID?,
    @field:NotBlank(message = "메시지를 입력해 주세요.")
    @field:Size(max = 500, message = "메시지는 최대 500자까지 입력할 수 있습니다.")
    val content: String,
) {
    fun toCommand(memberId: UUID) = SendChatMessageCommand(memberId = memberId, conversationId = conversationId, content = content)
}
