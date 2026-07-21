package com.yapp.todakun.notification.code

import com.yapp.todakun.common.code.ResponseCode

enum class NotificationErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    NOTIFICATION_NOT_FOUND("NOTIFICATION-404", "존재하지 않는 알림입니다.", 404),
    NOTIFICATION_FORBIDDEN("NOTIFICATION-403", "본인의 알림만 접근할 수 있습니다.", 403),
    PUSH_SEND_FAILED("NOTIFICATION-500", "푸시 알림 발송에 실패했습니다.", 500),
}
