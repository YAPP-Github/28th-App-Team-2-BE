package com.yapp.todakun.notification.port.inbound

data class PublishNoticeCommand(
    val title: String,
    val content: String,
    val deepLink: String?,
    /** 같은 내용을 의도적으로 재발송할 때만 지정한다. null이면 제목·본문·딥링크에서 키를 파생한다. */
    val idempotencyKey: String? = null,
)
