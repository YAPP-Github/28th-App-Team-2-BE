package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/**
 * AI를 통한 토닥이 답변 생성 실패(500).
 * 프롬프트 호출·스트리밍 중 오류 등 정상 흐름에서는 발생하면 안 되는 시스템 오류.
 */
class ChatGenerationFailedException(
    cause: Throwable? = null,
) : BusinessException(ChatErrorCode.CHAT_GENERATION_FAILED, cause)
