package com.yapp.todakun.notification.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.notification.code.NotificationErrorCode

/** FCM 발송 실패(500). 쿼터/서버 오류 등 재시도 불가한 실패에만 승격한다. */
class PushSendFailedException(
    cause: Throwable? = null,
) : BusinessException(NotificationErrorCode.PUSH_SEND_FAILED, cause)
