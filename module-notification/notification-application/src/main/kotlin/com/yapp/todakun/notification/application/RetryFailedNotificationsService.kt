package com.yapp.todakun.notification.application

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.notification.NotificationDeliveryFailure
import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.PushResult
import com.yapp.todakun.notification.policy.NotificationRetryPolicy
import com.yapp.todakun.notification.port.inbound.RetryFailedNotificationsUseCase
import com.yapp.todakun.notification.port.outbound.DispatchLockPort
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import org.springframework.stereotype.Service
import java.time.Instant

private const val PAGE_SIZE = 100

// pg_advisory_lock 키(임의 상수). NotificationDispatchService의 락 키들과 겹치지만 않으면 된다.
private const val RETRY_LOCK_KEY = 8_412_037_603L

/**
 * FCM 발송이 일시적으로 실패한 건을 재시도한다([NotificationRetryPolicy]: 최대 3회, 1분→5분→30분 지수 백오프).
 * 트랜잭션을 걸지 않는다 — 회원별 FCM 재호출(외부 I/O)을 DB 트랜잭션 밖에서 수행하기 위함이다(#41과 동일 원칙).
 * 재시도 대상은 [NotificationDeliveryFailure.failedTokens](일시 실패한 토큰만)와 재시도 시점의 최신
 * 디바이스 토큰의 교집합이다 — 이미 성공한 토큰을 다시 발송하지 않고, 로그아웃 등으로 사라진 토큰도 걸러낸다.
 * 대상 건별 처리는 코루틴(`async`/`awaitAll`)으로 병렬 실행하고([notificationDispatchDispatcher] 상한 4, [NotificationDispatchService]와 동일 인스턴스 공유),
 * `try`/`catch`로 예외를 먼저 격리한 뒤 `async`에 넘기므로(#81) 한 건의 실패가 `awaitAll()`을 통해 나머지 형제 코루틴을 취소시키거나 다른 건의 재시도를 중단시키지 않는다.
 * Blue/Green 배포 전환 구간의 중복 실행은 [dispatchLockPort]로 인스턴스 간 직렬화한다.
 */
@Service
@Loggable
class RetryFailedNotificationsService(
    private val notificationTransactionalStore: NotificationTransactionalStore,
    private val pushNotificationPort: PushNotificationPort,
    private val dispatchLockPort: DispatchLockPort,
    private val notificationMetrics: NotificationMetrics,
) : RetryFailedNotificationsUseCase {
    override fun retryDue() {
        dispatchLockPort.tryRun(RETRY_LOCK_KEY) {
            retryAllDue()
        } ?: log.info("다른 인스턴스가 이미 알림 재시도를 처리 중이라 스킵합니다.")
    }

    private fun retryAllDue() {
        val failures = notificationTransactionalStore.findDueDeliveryFailures(Instant.now(), PAGE_SIZE)
        runBlocking(notificationDispatchDispatcher) {
            failures.map { failure ->
                async {
                    runInterruptible {
                        try {
                            retry(failure)
                        } catch (e: Exception) {
                            log.error("알림 재시도 처리 실패: memberId=${failure.memberId}, type=${failure.type}", e)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun retry(failure: NotificationDeliveryFailure) {
        // 실패 등록 이후 로그아웃 등으로 사라졌을 수 있으니, 최신 토큰 목록과 교집합을 취한다.
        // 이미 성공한 토큰은 대상에서 빠지므로 같은 알림을 두 번 받지 않는다.
        val currentTokens = notificationTransactionalStore.getDeviceTokens(failure.memberId).associateBy { it.token }
        val tokens = failure.failedTokens.mapNotNull { currentTokens[it] }
        if (tokens.isEmpty()) {
            notificationTransactionalStore.deleteDeliveryFailure(failure.id)
            return
        }

        val results =
            try {
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
            } catch (e: Exception) {
                // 배치 호출 자체가 실패하면 결과를 알 수 없으니 이번에 시도한 토큰 전부를 여전히 실패로 간주한다.
                log.warn("알림 재시도 중 FCM 배치 호출이 실패했습니다: memberId=${failure.memberId}, type=${failure.type}", e)
                val stillFailing =
                    tokens.map {
                        PushResult(
                            token = it.token,
                            success = false,
                            errorCode = NotificationMetrics.ERROR_CODE_UNKNOWN,
                        )
                    }
                rescheduleOrGiveUp(failure, stillFailing)
                return
            }
        notificationTransactionalStore.cleanupExpiredTokens(results)

        val stillFailing = results.filter { !it.success && !it.tokenExpired }
        if (stillFailing.isEmpty()) {
            notificationTransactionalStore.deleteDeliveryFailure(failure.id)
            return
        }

        rescheduleOrGiveUp(failure, stillFailing)
    }

    private fun rescheduleOrGiveUp(
        failure: NotificationDeliveryFailure,
        stillFailing: List<PushResult>,
    ) {
        // 방금 실행한 이 시도까지 포함한 횟수 — attemptCount는 "이전에 이미 마친 재시도 횟수"라 +1을 더해야 한다.
        val attemptsMadeIncludingThis = failure.attemptCount + 1
        if (NotificationRetryPolicy.shouldGiveUp(attemptsMadeIncludingThis)) {
            val errorCode = stillFailing.firstOrNull()?.errorCode ?: NotificationMetrics.ERROR_CODE_UNKNOWN
            log.error("알림 발송 최종 실패(재시도 소진): memberId=${failure.memberId}, type=${failure.type}, errorCode=$errorCode")
            notificationMetrics.record(failure.type, NotificationDispatchResult.RETRY_EXHAUSTED, errorCode)
            notificationTransactionalStore.deleteDeliveryFailure(failure.id)
            return
        }

        notificationTransactionalStore.saveDeliveryFailure(
            failure.scheduleNextRetry(
                nextRetryAt = Instant.now().plus(NotificationRetryPolicy.backoffFor(attemptsMadeIncludingThis)),
                failedTokens = stillFailing.map { it.token },
            ),
        )
    }
}
