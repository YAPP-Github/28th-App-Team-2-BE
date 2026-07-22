package com.yapp.todakun.notification.adapter.web.controller

import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 알림 기능 엔드투엔드 검증(실 빈 사용, use case 미목킹): HTTP → 컨트롤러 → 서비스 → JPA → Postgres.
 * fcm.enabled=false라 FCM 발송은 NoOp이며, 인앱 알림함/설정/토큰 경로를 실제로 검증한다.
 */
@ExperimentalUuidApi
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class NotificationControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val sendNotificationPort: SendNotificationPort,
) : DescribeSpec(
        {
            fun auth(memberId: UUID) = authentication(UsernamePasswordAuthenticationToken(memberId, null, emptyList()))

            describe("GET /api/v1/notifications") {
                context("인증 없이 요청하면") {
                    it("401을 반환한다") {
                        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized)
                    }
                }

                context("알림이 발급된 인증 회원이 조회하면") {
                    it("목록·안읽음 수를 반환하고, 읽음 처리하면 안읽음이 0이 된다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        sendNotificationPort.send(
                            SendNotificationCommand(
                                memberId = memberId,
                                type = NotificationType.AI_COMPLETE,
                                title = "토닥이",
                                content = "질문에 대한 답변이 도착했어요",
                                push = false,
                            ),
                        )

                        val listBody =
                            objectMapper.readTree(
                                mockMvc
                                    .perform(get("/api/v1/notifications").with(auth(memberId)))
                                    .andExpect(status().isOk)
                                    .andReturn()
                                    .response.contentAsString,
                            )
                        listBody["data"]["unreadCount"].asLong() shouldBe 1L
                        listBody["data"]["notifications"][0]["type"].asString() shouldBe "AI_COMPLETE"
                        val notificationId = listBody["data"]["notifications"][0]["id"].asString()

                        mockMvc
                            .perform(patch("/api/v1/notifications/$notificationId/read").with(auth(memberId)))
                            .andExpect(status().isOk)

                        val afterBody =
                            objectMapper.readTree(
                                mockMvc
                                    .perform(get("/api/v1/notifications").with(auth(memberId)))
                                    .andExpect(status().isOk)
                                    .andReturn()
                                    .response.contentAsString,
                            )
                        afterBody["data"]["unreadCount"].asLong() shouldBe 0L
                    }
                }
            }

            describe("알림 설정") {
                it("기본값은 전부 OFF이며(받을 시간 08:00), PATCH로 변경된다") {
                    val memberId = Uuid.generateV7().toJavaUuid()

                    val defaultBody =
                        objectMapper.readTree(
                            mockMvc
                                .perform(get("/api/v1/notifications/settings").with(auth(memberId)))
                                .andExpect(status().isOk)
                                .andReturn()
                                .response.contentAsString,
                        )
                    defaultBody["data"]["morningReportEnabled"].asBoolean() shouldBe false
                    defaultBody["data"]["morningReportTime"].asString() shouldBe "08:00"

                    val request =
                        mapOf(
                            "morningReportEnabled" to true,
                            "morningReportTime" to "08:00",
                            "todakiEnabled" to true,
                            "luckyActionReminderEnabled" to false,
                        )
                    val patchedBody =
                        objectMapper.readTree(
                            mockMvc
                                .perform(
                                    patch("/api/v1/notifications/settings")
                                        .with(auth(memberId))
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request)),
                                ).andExpect(status().isOk)
                                .andReturn()
                                .response.contentAsString,
                        )
                    patchedBody["data"]["morningReportEnabled"].asBoolean() shouldBe true
                    patchedBody["data"]["todakiEnabled"].asBoolean() shouldBe true
                }
            }

            describe("POST /api/v1/notifications/device-tokens") {
                it("인증 회원이 FCM 토큰을 등록하면 200을 반환한다") {
                    val memberId = Uuid.generateV7().toJavaUuid()
                    val request = mapOf("token" to "fcm-registration-token-xyz", "platform" to "IOS")

                    mockMvc
                        .perform(
                            post("/api/v1/notifications/device-tokens")
                                .with(auth(memberId))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)),
                        ).andExpect(status().isOk)
                }
            }
        },
    )
