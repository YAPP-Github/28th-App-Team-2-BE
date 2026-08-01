package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/** 사용자 메시지 본문이 최대 길이(500자)를 초과한 경우(400). */
class ChatContentTooLongException :
    BusinessException(ChatErrorCode.CHAT_CONTENT_TOO_LONG)
