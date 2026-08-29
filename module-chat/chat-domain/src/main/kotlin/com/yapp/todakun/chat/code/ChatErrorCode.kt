package com.yapp.todakun.chat.code

import com.yapp.todakun.common.code.ResponseCode

enum class ChatErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    CHAT_CONTENT_TOO_LONG("CHAT-400", "메시지는 최대 500자까지 입력할 수 있습니다.", 400),
    CHAT_CONVERSATION_NOT_FOUND("CHAT-404", "대화를 찾을 수 없습니다.", 404),
    CHAT_CONVERSATION_FORBIDDEN("CHAT-403", "해당 대화에 접근할 권한이 없습니다.", 403),
    CHAT_DAILY_QUOTA_EXCEEDED("CHAT-429", "오늘 무료 채팅 횟수를 모두 사용했습니다.", 429),
    CHAT_GENERATION_FAILED("CHAT-500", "토닥이 답변 생성에 실패했습니다.", 500),
    CHAT_EMPTY_RESPONSE("CHAT-500", "AI로부터 빈 응답을 받았습니다.", 500),
    CHAT_ASSISTANT_MESSAGE_NOT_FOUND("CHAT-500", "어시스턴트 메시지를 찾을 수 없습니다.", 500),
    CHAT_STREAM_UNAVAILABLE("CHAT-503", "지금은 요청이 많아 토닥이가 답변할 수 없습니다. 잠시 후 다시 시도해 주세요.", 503),
}
