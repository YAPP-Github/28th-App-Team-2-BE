package com.yapp.todakun.chat.exception

import com.yapp.todakun.chat.code.ChatErrorCode
import com.yapp.todakun.common.exception.BusinessException

/** 하루 무료 채팅 횟수(기본 3회)를 모두 소진한 경우(429). */
class ChatDailyQuotaExceededException :
    BusinessException(ChatErrorCode.CHAT_DAILY_QUOTA_EXCEEDED)
