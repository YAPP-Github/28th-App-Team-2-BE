package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/** 대화 ID로 조회했으나 존재하지 않는 경우(404). */
class ChatConversationNotFoundException :
    BusinessException(ChatErrorCode.CHAT_CONVERSATION_NOT_FOUND)
