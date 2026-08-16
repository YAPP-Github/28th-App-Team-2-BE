package com.yapp.todakun.notification

/** 공지(NOTICE) 발송 시 지정하는 세부 분류. 발송 이력(NoticeDispatchHistory)에만 반영되며 push payload에는 영향을 주지 않는다. */
enum class NoticeType {
    GENERAL,
    EVENT,
    MAINTENANCE,
    UPDATE,
}
