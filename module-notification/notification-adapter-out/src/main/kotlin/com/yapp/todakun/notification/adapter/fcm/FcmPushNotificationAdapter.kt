package com.yapp.todakun.notification.adapter.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.PushResult
import com.yapp.todakun.notification.exception.PushSendFailedException
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import com.google.firebase.messaging.Notification as FcmNotification

/**
 * FCM 실제 발송 어댑터. Message 빌드·전송·에러코드 해석은 모두 이 어댑터 안에서 끝난다.
 * 등록 해제된 토큰(UNREGISTERED)만 실패가 아니라 "정리 대상"으로 보고한다(tokenExpired).
 * INVALID_ARGUMENT는 잘못된 토큰뿐 아니라 payload 오류로도 발생할 수 있어 만료로 단정하지 않는다(정상 토큰 오삭제 방지).
 * 단건([send])은 그 외 오류를 예외로 승격하지만, 배치([sendAll])는 부분 실패가 성공 건의 후속 처리(이력 저장·토큰 정리)를
 * 롤백시키지 않도록 실패를 예외 대신 결괏값(success=false)으로 반환한다. 이때 원인([MessagingErrorCode])을 결과에
 * 담고 warn 로그로도 남긴다 — 그렇지 않으면 재시도가 모두 소진된 뒤 원인을 알 방법이 완전히 사라진다.
 */
@Component
@ConditionalOnProperty(prefix = "fcm", name = ["enabled"], havingValue = "true")
@Loggable
class FcmPushNotificationAdapter(
    private val firebaseMessaging: FirebaseMessaging,
) : PushNotificationPort {
    override fun send(notification: PushNotification): PushResult =
        try {
            firebaseMessaging.send(notification.toMessage())
            PushResult(token = notification.token, success = true)
        } catch (e: FirebaseMessagingException) {
            if (e.isTokenExpired()) {
                PushResult(token = notification.token, success = false, tokenExpired = true)
            } else {
                throw PushSendFailedException(e)
            }
        }

    override fun sendAll(notifications: List<PushNotification>): List<PushResult> {
        if (notifications.isEmpty()) return emptyList()
        val batch = firebaseMessaging.sendEach(notifications.map { it.toMessage() })
        return notifications.zip(batch.responses, ::toPushResult).also(::logFailures)
    }

    private fun toPushResult(
        notification: PushNotification,
        response: SendResponse,
    ): PushResult =
        when {
            response.isSuccessful -> PushResult(token = notification.token, success = true)
            response.exception?.isTokenExpired() == true ->
                PushResult(token = notification.token, success = false, tokenExpired = true)
            else ->
                PushResult(
                    token = notification.token,
                    success = false,
                    // FCM 고유 코드(messagingErrorCode)는 서버가 구조화된 FCM 에러를 반환했을 때만 채워진다.
                    // 5xx/타임아웃처럼 일반 HTTP 레벨 오류면 null이라, 그 경우 일반 오류 코드(errorCode)로 대체해 로그에서 원인이 완전히 사라지지 않게 한다.
                    errorCode = response.exception?.messagingErrorCode?.name ?: response.exception?.errorCode?.name,
                )
        }

    private fun logFailures(results: List<PushResult>) {
        results.filter { !it.success && !it.tokenExpired }.forEach {
            log.warn("FCM 배치 발송 실패: errorCode=${it.errorCode}")
        }
    }

    private fun FirebaseMessagingException.isTokenExpired(): Boolean = messagingErrorCode == MessagingErrorCode.UNREGISTERED

    // setToken()은 FCM이 등록 토큰 대신 Firebase Installation ID(FID)로 전환하며 deprecated됐지만(setFid()),
    // FID는 등록 토큰과 다른 식별자라 client가 FID 발급·전송 로직을 갖추기 전엔 그냥 바꿔 끼울 수 없다.
    // 두 경로가 전환 기간 동안 병행 지원되므로 지금은 억제만 하고, 마이그레이션은 클라이언트 협의가 필요한 별도 작업으로 남긴다.
    @Suppress("DEPRECATION")
    private fun PushNotification.toMessage(): Message =
        Message
            .builder()
            .setToken(token)
            .setNotification(FcmNotification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .build()
}
