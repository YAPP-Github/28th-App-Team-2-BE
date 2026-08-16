package com.yapp.todakun.chat.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.port.inbound.ChatStreamListener
import com.yapp.todakun.chat.port.inbound.ChatTurnStarted
import com.yapp.todakun.chat.port.inbound.StreamChatAnswerUseCase
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val USER_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003")
private val ASSISTANT_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000004")
private const val AWAIT_TIMEOUT_SECONDS = 5L

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class ChatStreamControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var streamChatAnswerUseCase: StreamChatAnswerUseCase

    init {
        afterTest { clearMocks(streamChatAnswerUseCase) }

        describe("POST /api/v1/chat/messages") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc
                        .perform(
                            post("/api/v1/chat/messages")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(mapOf("conversationId" to null, "content" to "안녕"))),
                        ).andExpect(status().isUnauthorized)

                    verify(exactly = 0) { streamChatAnswerUseCase.stream(any(), any()) }
                }
            }

            context("메시지 내용이 비어 있으면") {
                it("400을 반환한다") {
                    mockMvc
                        .perform(
                            post("/api/v1/chat/messages")
                                .with(authenticatedMember())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(mapOf("conversationId" to null, "content" to ""))),
                        ).andExpect(status().isBadRequest)

                    verify(exactly = 0) { streamChatAnswerUseCase.stream(any(), any()) }
                }
            }

            context("메시지 내용이 500자를 초과하면") {
                it("400을 반환한다") {
                    val tooLongContent = "가".repeat(501)

                    mockMvc
                        .perform(
                            post("/api/v1/chat/messages")
                                .with(authenticatedMember())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(mapOf("conversationId" to null, "content" to tooLongContent))),
                        ).andExpect(status().isBadRequest)

                    verify(exactly = 0) { streamChatAnswerUseCase.stream(any(), any()) }
                }
            }

            context("인증된 회원이 유효한 메시지를 보내면") {
                it("SSE로 start·delta·done 이벤트를 순서대로 전송한다") {
                    val proceedLatch = CountDownLatch(1)
                    val doneLatch = CountDownLatch(1)
                    every { streamChatAnswerUseCase.stream(any(), any()) } answers {
                        val listener = secondArg<ChatStreamListener>()
                        proceedLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        val started =
                            ChatTurnStarted(CONVERSATION_ID, USER_MESSAGE_ID, ASSISTANT_MESSAGE_ID, quotaUsed = 1, quotaLimit = 10)
                        listener.onStart(started)
                        listener.onDelta("안녕하세요")
                        listener.onDone(ASSISTANT_MESSAGE_ID)
                        doneLatch.countDown()
                    }

                    val mvcResult =
                        mockMvc
                            .perform(
                                post("/api/v1/chat/messages")
                                    .with(authenticatedMember())
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(mapOf("conversationId" to null, "content" to "안녕"))),
                            ).andExpect(request().asyncStarted())
                            .andReturn()

                    proceedLatch.countDown()
                    doneLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe true

                    val body =
                        mockMvc
                            .perform(asyncDispatch(mvcResult))
                            .andExpect(status().isOk)
                            .andReturn()
                            .response.contentAsString

                    body shouldContain "event:start"
                    body shouldContain "event:delta"
                    body shouldContain "event:done"
                    body shouldContain ASSISTANT_MESSAGE_ID.toString()
                }
            }

            context("스트리밍 중 오류가 발생하면") {
                it("SSE로 error 이벤트를 전송한다") {
                    val proceedLatch = CountDownLatch(1)
                    val doneLatch = CountDownLatch(1)
                    every { streamChatAnswerUseCase.stream(any(), any()) } answers {
                        val listener = secondArg<ChatStreamListener>()
                        proceedLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        listener.onError(ChatConversationForbiddenException())
                        doneLatch.countDown()
                    }

                    val mvcResult =
                        mockMvc
                            .perform(
                                post("/api/v1/chat/messages")
                                    .with(authenticatedMember())
                                    .contentType("application/json")
                                    .content(
                                        objectMapper.writeValueAsString(mapOf("conversationId" to CONVERSATION_ID, "content" to "안녕")),
                                    ),
                            ).andExpect(request().asyncStarted())
                            .andReturn()

                    proceedLatch.countDown()
                    doneLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe true

                    val body =
                        mockMvc
                            .perform(asyncDispatch(mvcResult))
                            .andExpect(status().isOk)
                            .andReturn()
                            .response.contentAsString

                    body shouldContain "event:error"
                    body shouldContain "CHAT-403"
                }
            }
        }
    }

    private fun authenticatedMember() = authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList()))
}
