package com.yapp.todakun.notification.policy

import java.time.Duration

/**
 * 알림 발송 재시도 정책: 최대 3회, 1분→5분→30분 지수 백오프.
 * 현재 재시도 대상은 "무효 토큰(UNREGISTERED)이 아닌 모든 실패"다 — INVALID_ARGUMENT처럼 재시도해도
 * 성공할 수 없는 영구 오류까지 포함되므로, 오류 분류를 세분화하기 전까지는 백오프 횟수만큼 낭비가 발생한다.
 * 무효 토큰(UNREGISTERED 등)은 이 정책 대상이 아니다 — 재시도 없이 즉시 토큰을 정리한다.
 */
object NotificationRetryPolicy {
    private val BACKOFF = listOf(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30))

    val maxAttempts: Int = BACKOFF.size

    /** [attemptCount]번째(0-base, 이미 수행한 재시도 횟수) 다음 재시도까지의 대기 시간. */
    fun backoffFor(attemptCount: Int): Duration = BACKOFF[attemptCount]

    /** 이미 [attemptCount]회 재시도했다면 더 이상 재시도하지 않고 포기해야 하는지. */
    fun shouldGiveUp(attemptCount: Int): Boolean = attemptCount >= maxAttempts
}
