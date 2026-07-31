package com.yapp.todakun.chat.adapter.web.controller

import com.yapp.todakun.chat.adapter.config.ChatStreamConfig.Companion.CHAT_STREAM_EXECUTOR_BEAN_NAME
import com.yapp.todakun.chat.adapter.web.ChatStreamApi
import com.yapp.todakun.chat.adapter.web.dto.request.SendChatMessageRequest
import com.yapp.todakun.chat.adapter.web.sse.SseEmitterChatStreamListener
import com.yapp.todakun.chat.port.inbound.StreamChatAnswerUseCase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

private const val SSE_TIMEOUT_MS = 120_000L

@RestController
class ChatStreamController(
    private val streamChatAnswerUseCase: StreamChatAnswerUseCase,
    @Qualifier(CHAT_STREAM_EXECUTOR_BEAN_NAME)
    private val chatStreamTaskExecutor: AsyncTaskExecutor,
) : ChatStreamApi {
    @ExperimentalUuidApi
    override fun sendMessage(
        request: SendChatMessageRequest,
        memberId: UUID,
    ): SseEmitter {
        val emitter = SseEmitter(SSE_TIMEOUT_MS)
        val listener = SseEmitterChatStreamListener(emitter)

        // SSE 연결은 즉시 반환하고, 실제 생성은 전용 워커 스레드에서 처리한다(요청 스레드를 오래 붙잡지 않는다).
        chatStreamTaskExecutor.execute {
            streamChatAnswerUseCase.stream(request.toCommand(memberId), listener)
        }

        return emitter
    }
}
