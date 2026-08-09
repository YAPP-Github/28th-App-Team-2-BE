package com.yapp.todakun.notification.port.outbound

/**
 * 다중 인스턴스(Blue/Green) 환경에서 스케줄 발송 작업의 중복 실행을 막는 세션 스코프 advisory lock.
 * 이미 다른 인스턴스가 [key]를 보유 중이면 즉시 null을 반환하고 [block]을 실행하지 않는다(non-blocking).
 */
interface DispatchLockPort {
    fun <T> tryRun(
        key: Long,
        block: () -> T,
    ): T?
}
