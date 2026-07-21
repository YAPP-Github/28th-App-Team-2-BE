package com.yapp.todakun.notification.adapter.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.PushResult
import com.yapp.todakun.notification.exception.PushSendFailedException
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import com.google.firebase.messaging.Notification as FcmNotification

/**
 * FCM 실제 발송 어댑터. Message 빌드·전송·에러코드 해석은 모두 이 어댑터 안에서 끝난다.
 * 등록 해제/무효 토큰(UNREGISTERED/INVALID_ARGUMENT)은 실패가 아니라 "정리 대상"으로 보고하고(tokenExpired),
 * 그 외 오류만 예외로 승격한다.
 */
@Component
@ConditionalOnProperty(prefix = "fcm", name = ["enabled"], havingValue = "true")
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
        return notifications.mapIndexed { i, notification ->
            val response = batch.responses[i]
            when {
                response.isSuccessful -> PushResult(token = notification.token, success = true)
                response.exception?.isTokenExpired() == true ->
                    PushResult(token = notification.token, success = false, tokenExpired = true)
                else -> throw PushSendFailedException(response.exception)
            }
        }
    }

    private fun FirebaseMessagingException.isTokenExpired(): Boolean =
        messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
            messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT

    private fun PushNotification.toMessage(): Message =
        Message
            .builder()
            .setToken(token)
            .setNotification(FcmNotification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .build()
}
