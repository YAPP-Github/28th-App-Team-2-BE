package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.NotificationDeliveryFailure
import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.policy.NotificationRetryPolicy
import com.yapp.todakun.notification.port.inbound.RetryFailedNotificationsUseCase
import com.yapp.todakun.notification.port.outbound.DispatchLockPort
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

private const val PAGE_SIZE = 100

// pg_advisory_lock 키(임의 상수). NotificationDispatchService의 락 키들과 겹치지만 않으면 된다.
private const val RETRY_LOCK_KEY = 8_412_037_603L

/**
 * FCM 발송이 일시적으로 실패한 건을 재시도한다([NotificationRetryPolicy]: 최대 3회, 1분→5분→30분 지수 백오프).
 * 트랜잭션을 걸지 않는다 — 회원별 FCM 재호출(외부 I/O)을 DB 트랜잭션 밖에서 수행하기 위함이다(#41과 동일 원칙).
 * 재시도 시점의 최신 디바이스 토큰을 다시 조회한다 — 실패 등록 이후 로그아웃 등으로 토큰이 바뀌었을 수 있어
 * 등록 시점의 토큰을 캐시해두지 않는다.
 * 대상 건별 처리는 예외를 격리해 한 건의 실패가 나머지 건의 재시도를 중단시키지 않는다.
 * Blue/Green 배포 전환 구간의 중복 실행은 [dispatchLockPort]로 인스턴스 간 직렬화한다.
 */
@Service
class RetryFailedNotificationsService(
    private val notificationTransactionalStore: NotificationTransactionalStore,
    private val pushNotificationPort: PushNotificationPort,
    private val dispatchLockPort: DispatchLockPort,
    private val notificationMetrics: NotificationMetrics,
) : RetryFailedNotificationsUseCase {
    override fun retryDue() {
        dispatchLockPort.tryRun(RETRY_LOCK_KEY) {
            notificationTransactionalStore.findDueDeliveryFailures(Instant.now(), PAGE_SIZE).forEach { failure ->
                try {
                    retry(failure)
                } catch (e: Exception) {
                    log.error("알림 재시도 처리 실패: memberId=${failure.memberId}, type=${failure.type}", e)
                }
            }
        } ?: log.info("다른 인스턴스가 이미 알림 재시도를 처리 중이라 스킵합니다.")
    }

    private fun retry(failure: NotificationDeliveryFailure) {
        val tokens = notificationTransactionalStore.getDeviceTokens(failure.memberId)
        if (tokens.isEmpty()) {
            notificationTransactionalStore.deleteDeliveryFailure(failure.id)
            return
        }

        val results =
            pushNotificationPort.sendAll(
                tokens.map { token ->
                    PushNotification(
                        token = token.token,
                        title = failure.title,
                        body = failure.content,
                        data = buildPushData(failure.type, failure.notificationId, failure.deepLink),
                    )
                },
            )
        notificationTransactionalStore.cleanupExpiredTokens(results)

        if (results.none { !it.success && !it.tokenExpired }) {
            notificationTransactionalStore.deleteDeliveryFailure(failure.id)
            return
        }

        // 방금 실행한 이 시도까지 포함한 횟수 — attemptCount는 "이전에 이미 마친 재시도 횟수"라 +1을 더해야 한다.
        val attemptsMadeIncludingThis = failure.attemptCount + 1
        if (NotificationRetryPolicy.shouldGiveUp(attemptsMadeIncludingThis)) {
            log.error("알림 발송 최종 실패(재시도 소진): memberId=${failure.memberId}, type=${failure.type}")
            notificationMetrics.record(failure.type, NotificationDispatchResult.RETRY_EXHAUSTED)
            notificationTransactionalStore.deleteDeliveryFailure(failure.id)
            return
        }

        notificationTransactionalStore.saveDeliveryFailure(
            failure.scheduleNextRetry(Instant.now().plus(NotificationRetryPolicy.backoffFor(failure.attemptCount))),
        )
    }
}

private val log = LoggerFactory.getLogger(RetryFailedNotificationsService::class.java)
