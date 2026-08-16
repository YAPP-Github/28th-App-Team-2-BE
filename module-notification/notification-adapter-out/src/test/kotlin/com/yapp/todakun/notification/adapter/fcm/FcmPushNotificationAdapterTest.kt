package com.yapp.todakun.notification.adapter.fcm

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.exception.PushSendFailedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class FcmPushNotificationAdapterTest :
    DescribeSpec(
        {
            val firebaseMessaging = mockk<FirebaseMessaging>()
            val adapter = FcmPushNotificationAdapter(firebaseMessaging)

            afterTest { clearMocks(firebaseMessaging) }

            describe("send") {
                context("등록 해제된 토큰이면(UNREGISTERED)") {
                    it("실패가 아니라 정리 대상(tokenExpired)으로 보고한다") {
                        val exception = mockk<FirebaseMessagingException>()
                        every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
                        every { firebaseMessaging.send(any()) } throws exception

                        val result = adapter.send(PushNotification("stale", "제목", "본문"))

                        result.success shouldBe false
                        result.tokenExpired shouldBe true
                    }
                }

                context("쿼터/서버 오류 등 그 외 실패면") {
                    it("PushSendFailedException으로 승격한다") {
                        val exception = mockk<FirebaseMessagingException>()
                        every { exception.messagingErrorCode } returns MessagingErrorCode.INTERNAL
                        every { firebaseMessaging.send(any()) } throws exception

                        shouldThrow<PushSendFailedException> {
                            adapter.send(PushNotification("token", "제목", "본문"))
                        }
                    }
                }

                context("정상 발송이면") {
                    it("성공으로 보고한다") {
                        every { firebaseMessaging.send(any()) } returns "message-id"

                        val result = adapter.send(PushNotification("token", "제목", "본문"))

                        result.success shouldBe true
                        result.tokenExpired shouldBe false
                    }
                }
            }

            describe("sendAll") {
                context("일부 토큰이 등록 해제(UNREGISTERED)로 실패하면") {
                    it("정리 대상(tokenExpired)으로 보고하고 errorCode는 채우지 않는다") {
                        val response = mockk<SendResponse>()
                        val exception = mockk<FirebaseMessagingException>()
                        every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
                        every { response.isSuccessful } returns false
                        every { response.exception } returns exception
                        every { firebaseMessaging.sendEach(any()) } returns mockBatchResponse(listOf(response))

                        val results = adapter.sendAll(listOf(PushNotification("stale", "제목", "본문")))

                        results[0].tokenExpired shouldBe true
                        results[0].errorCode shouldBe null
                    }
                }

                context("등록 해제가 아닌 다른 이유로 실패하면") {
                    it("실패로 보고하고 FCM 에러코드를 errorCode에 담는다") {
                        val response = mockk<SendResponse>()
                        val exception = mockk<FirebaseMessagingException>()
                        every { exception.messagingErrorCode } returns MessagingErrorCode.UNAVAILABLE
                        every { response.isSuccessful } returns false
                        every { response.exception } returns exception
                        every { firebaseMessaging.sendEach(any()) } returns mockBatchResponse(listOf(response))

                        val results = adapter.sendAll(listOf(PushNotification("flaky", "제목", "본문")))

                        results[0].success shouldBe false
                        results[0].tokenExpired shouldBe false
                        results[0].errorCode shouldBe "UNAVAILABLE"
                    }
                }

                context("정상 발송이면") {
                    it("성공으로 보고하고 errorCode는 채우지 않는다") {
                        val response = mockk<SendResponse>()
                        every { response.isSuccessful } returns true
                        every { firebaseMessaging.sendEach(any()) } returns mockBatchResponse(listOf(response))

                        val results = adapter.sendAll(listOf(PushNotification("token", "제목", "본문")))

                        results[0].success shouldBe true
                        results[0].errorCode shouldBe null
                    }
                }
            }
        },
    )

private fun mockBatchResponse(responses: List<SendResponse>): BatchResponse {
    val batch = mockk<BatchResponse>()
    every { batch.responses } returns responses
    return batch
}
