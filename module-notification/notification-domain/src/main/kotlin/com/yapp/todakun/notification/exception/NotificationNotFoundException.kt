package com.yapp.todakun.notification.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.notification.code.NotificationErrorCode

/** 존재하지 않는 알림(404). */
class NotificationNotFoundException : NotFoundException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)
