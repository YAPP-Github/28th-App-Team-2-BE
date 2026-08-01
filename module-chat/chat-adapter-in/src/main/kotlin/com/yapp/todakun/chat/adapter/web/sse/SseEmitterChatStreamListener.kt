package com.yapp.todakun.chat.adapter.web.sse

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.port.inbound.ChatStreamListener
import com.yapp.todakun.chat.port.inbound.ChatTurnStarted
import com.yapp.todakun.common.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private const val UNKNOWN_ERROR_CODE = "CHAT-500"
private const val UNKNOWN_ERROR_MESSAGE = "일시적인 오류가 발생했습니다."

/**
 * [ChatStreamListener]를 Spring MVC [SseEmitter]에 연결하는 어댑터. 전송 실패(클라이언트 연결 종료)를 감지해도
 * 예외를 상위로 전파하지 않는다 — 이 아키텍처는 연결이 끊겨도 생성·저장·알림을 끝까지 진행하기 때문이다(끊김은 오류가 아니라 정상 경로).
 */
class SseEmitterChatStreamListener(
    private val emitter: SseEmitter,
) : ChatStreamListener {
    private val log = LoggerFactory.getLogger(javaClass)
    private val disconnected = AtomicBoolean(false)

    init {
        emitter.onCompletion { disconnected.set(true) }
        emitter.onTimeout { disconnected.set(true) }
        emitter.onError { disconnected.set(true) }
    }

    override fun onStart(started: ChatTurnStarted) = send("start", ChatStartEvent.from(started))

    override fun onDelta(text: String) = send("delta", ChatDeltaEvent(text))

    override fun onAction(action: ChatAction) = send("action", ChatActionEvent.from(action))

    override fun onDone(assistantMessageId: UUID) {
        send("done", ChatDoneEvent(assistantMessageId))
        emitter.complete()
    }

    override fun onError(error: Throwable) {
        val errorCode = (error as? BusinessException)?.errorCode
        if (errorCode == null) {
            log.error("채팅 스트림에서 알 수 없는 오류가 발생했습니다", error)
        }
        send("error", ChatErrorEvent(errorCode?.code ?: UNKNOWN_ERROR_CODE, errorCode?.message ?: UNKNOWN_ERROR_MESSAGE))
        emitter.complete()
    }

    override fun isClientConnected(): Boolean = !disconnected.get()

    private fun send(
        eventName: String,
        data: Any,
    ) {
        if (disconnected.get()) return

        try {
            emitter.send(SseEmitter.event().name(eventName).data(data))
        } catch (e: IOException) {
            disconnected.set(true)
        } catch (e: IllegalStateException) {
            // SseEmitter가 이미 완료/타임아웃된 뒤 send가 호출된 경우(비-I/O 오류). 끊김으로 처리한다.
            disconnected.set(true)
        }
    }
}
