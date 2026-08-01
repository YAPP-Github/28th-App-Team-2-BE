package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/** 준비 단계에서 이미 생성해 둔 어시스턴트 메시지가 사라진 경우(500, 정상 흐름에서는 발생하면 안 되는 내부 불변식 위반). */
class ChatAssistantMessageNotFoundException :
    BusinessException(ChatErrorCode.CHAT_ASSISTANT_MESSAGE_NOT_FOUND)
