package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.port.inbound.ChatStreamListener
import com.yapp.todakun.chat.port.inbound.ChatTurnStarted
import com.yapp.todakun.chat.port.inbound.SendChatMessageCommand
import com.yapp.todakun.chat.port.inbound.StreamChatAnswerUseCase
import com.yapp.todakun.chat.port.outbound.ChatAiPort
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import org.springframework.stereotype.Service
import kotlin.uuid.ExperimentalUuidApi

private const val NOTIFICATION_CONTENT_MAX_LENGTH = 100
private const val DEEP_LINK_PREFIX = "todakun://chat/conversations/"

/**
 * 답변 스트리밍 오케스트레이터. 의도적으로 `@CommandService`가 아닌 평범한 `@Service`다 —
 * AI 스트리밍 호출이 길게 걸리는 구간을 트랜잭션(DB 커넥션)으로 감싸지 않기 위해, 준비/완료/실패를
 * 각각 짧은 트랜잭션([PrepareChatTurnService]/[CompleteChatTurnService]/[FailChatTurnService])으로 쪼개 호출한다.
 *
 * SSE 연결은 컨트롤러가 이미 열어둔 뒤 이 메서드를 호출하므로, 준비 단계 실패(쿼터 소진·대화 없음/권한 등)도
 * HTTP 상태 코드가 아니라 [listener]의 `error` 이벤트로만 전달된다(동기 SSE + 생성 스레드 분리 아키텍처의 트레이드오프).
 */
@Service
class StreamChatAnswerService(
    private val prepareChatTurnService: PrepareChatTurnService,
    private val completeChatTurnService: CompleteChatTurnService,
    private val failChatTurnService: FailChatTurnService,
    private val chatAiPort: ChatAiPort,
    private val sendNotificationPort: SendNotificationPort,
) : StreamChatAnswerUseCase {
    @ExperimentalUuidApi
    override fun stream(
        command: SendChatMessageCommand,
        listener: ChatStreamListener,
    ) {
        val prepared =
            try {
                prepareChatTurnService.prepare(command)
            } catch (e: Exception) {
                listener.onError(e)
                return
            }

        listener.onStart(
            ChatTurnStarted(
                conversationId = prepared.conversationId,
                userMessageId = prepared.userMessageId,
                assistantMessageId = prepared.assistantMessageId,
                quotaUsed = prepared.quota.used,
                quotaLimit = prepared.quota.limit,
            ),
        )

        // 실패를 도메인 예외로 번역하는 책임은 어댑터([com.yapp.todakun.chat.adapter.ai.VertexAiChatAdapter])에 있다.
        // 여기서는 실패 시 보상 동작(메시지 실패 처리·쿼터 환불)만 담당한다.
        val answer =
            try {
                chatAiPort.streamAnswer(prepared.promptContext) { delta -> listener.onDelta(delta) }
            } catch (e: Exception) {
                failChatTurnService.fail(prepared.memberId, prepared.assistantMessageId)
                listener.onError(e)
                return
            }

        // 액션 카드 추출은 부가 기능이라 실패해도 이미 만들어진 답변 자체를 무효로 만들지 않는다.
        val action = runCatching { chatAiPort.extractAction(prepared.promptContext, answer) }.getOrNull()
        val connected = listener.isClientConnected()

        completeChatTurnService.complete(prepared.conversationId, prepared.assistantMessageId, answer, action, unread = !connected)

        action?.let(listener::onAction)
        listener.onDone(prepared.assistantMessageId)

        sendNotificationPort.send(
            SendNotificationCommand(
                memberId = prepared.memberId,
                type = NotificationType.AI_COMPLETE,
                title = prepared.conversationTitle,
                content = answer.take(NOTIFICATION_CONTENT_MAX_LENGTH),
                deepLink = "$DEEP_LINK_PREFIX${prepared.conversationId}",
                push = !connected,
            ),
        )
    }
}
