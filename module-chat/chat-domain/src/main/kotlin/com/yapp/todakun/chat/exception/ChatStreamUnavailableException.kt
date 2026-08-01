package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/** 스트리밍 전용 워커 풀이 포화되어 작업을 받아들일 수 없는 경우(503). */
class ChatStreamUnavailableException(
    cause: Throwable? = null,
) : BusinessException(ChatErrorCode.CHAT_STREAM_UNAVAILABLE, cause)
