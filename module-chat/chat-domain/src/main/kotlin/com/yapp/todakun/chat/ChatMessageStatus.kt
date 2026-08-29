package com.yapp.todakun.chat

/** 어시스턴트 메시지의 생성 진행 상태. USER 메시지는 항상 [COMPLETED]다. */
enum class ChatMessageStatus {
    GENERATING,
    COMPLETED,
    FAILED,
}
