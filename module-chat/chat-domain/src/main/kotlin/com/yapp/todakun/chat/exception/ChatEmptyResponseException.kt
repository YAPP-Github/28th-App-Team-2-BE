package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/** AI가 빈 응답(공백만 있는 스트림)을 반환한 경우(500). */
class ChatEmptyResponseException :
    BusinessException(ChatErrorCode.CHAT_EMPTY_RESPONSE)
