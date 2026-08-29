package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/** 대화가 존재하지만 요청 회원의 소유가 아닌 경우(403). */
class ChatConversationForbiddenException :
    BusinessException(ChatErrorCode.CHAT_CONVERSATION_FORBIDDEN)
