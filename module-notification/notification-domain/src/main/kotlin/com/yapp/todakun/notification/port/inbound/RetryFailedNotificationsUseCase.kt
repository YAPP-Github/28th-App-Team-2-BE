package com.yapp.todakun.notification.port.inbound

/** 스케줄러(driving adapter)가 매분 호출하는, 발송이 일시적으로 실패한 알림의 재시도 유스케이스. */
interface RetryFailedNotificationsUseCase {
    fun retryDue()
}
