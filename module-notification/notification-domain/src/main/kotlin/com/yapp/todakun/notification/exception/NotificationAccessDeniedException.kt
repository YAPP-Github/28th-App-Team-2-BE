package com.yapp.todakun.notification.exception

import com.yapp.todakun.common.exception.ForbiddenException
import com.yapp.todakun.notification.code.NotificationErrorCode

/** 타인의 알림 접근(403). */
class NotificationAccessDeniedException : ForbiddenException(NotificationErrorCode.NOTIFICATION_FORBIDDEN)
