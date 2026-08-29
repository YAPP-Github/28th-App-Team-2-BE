package com.yapp.todakun.chat.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.chat.ChatMessageRole
import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.port.inbound.ChatEntryResult
import com.yapp.todakun.chat.port.inbound.ChatMessageResult
import com.yapp.todakun.chat.port.inbound.ConversationDetailResult
import com.yapp.todakun.chat.port.inbound.ConversationListResult
import com.yapp.todakun.chat.port.inbound.ConversationSummaryResult
import com.yapp.todakun.chat.port.inbound.DeleteConversationUseCase
import com.yapp.todakun.chat.port.inbound.GetChatEntryUseCase
import com.yapp.todakun.chat.port.inbound.GetConversationsUseCase
import com.yapp.todakun.chat.port.inbound.OpenConversationUseCase
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003")

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class ChatControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getChatEntryUseCase: GetChatEntryUseCase

    @MockkBean
    private lateinit var getConversationsUseCase: GetConversationsUseCase

    @MockkBean
    private lateinit var openConversationUseCase: OpenConversationUseCase

    @MockkBean
    private lateinit var deleteConversationUseCase: DeleteConversationUseCase

    init {
        afterTest {
            clearMocks(getChatEntryUseCase, getConversationsUseCase, openConversationUseCase, deleteConversationUseCase)
        }

        describe("GET /api/v1/chat/entry") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/chat/entry")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getChatEntryUseCase.getEntry(any()) }
                }
            }

            context("인증된 회원이 요청하면") {
                it("200과 함께 진입 화면 정보를 반환한다") {
                    every { getChatEntryUseCase.getEntry(MEMBER_ID) } returns ChatEntryResult.of(quotaUsed = 3, quotaLimit = 10)

                    val data = successData(mockMvc.get("/api/v1/chat/entry") { with(authenticatedMember()) })

                    data["quota"]["used"].asInt() shouldBe 3
                    data["quota"]["limit"].asInt() shouldBe 10
                    verify(exactly = 1) { getChatEntryUseCase.getEntry(MEMBER_ID) }
                }
            }
        }

        describe("GET /api/v1/chat/conversations") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/chat/conversations")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getConversationsUseCase.getConversations(any()) }
                }
            }

            context("인증된 회원이 요청하면") {
                it("200과 함께 대화 목록을 반환한다") {
                    val summary =
                        ConversationSummaryResult(
                            id = CONVERSATION_ID,
                            title = "오늘 하루 어때요?",
                            lastMessageAt = Instant.parse("2026-08-16T00:00:00Z"),
                            unread = true,
                        )
                    every { getConversationsUseCase.getConversations(MEMBER_ID) } returns ConversationListResult(listOf(summary))

                    val data = successData(mockMvc.get("/api/v1/chat/conversations") { with(authenticatedMember()) })

                    data["conversations"][0]["id"].asString() shouldBe CONVERSATION_ID.toString()
                    data["conversations"][0]["title"].asString() shouldBe "오늘 하루 어때요?"
                    data["conversations"][0]["unread"].asBoolean() shouldBe true
                    verify(exactly = 1) { getConversationsUseCase.getConversations(MEMBER_ID) }
                }
            }
        }

        describe("GET /api/v1/chat/conversations/{conversationId}") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/chat/conversations/$CONVERSATION_ID")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { openConversationUseCase.open(any(), any()) }
                }
            }

            context("인증된 회원이 본인 소유 대화를 조회하면") {
                it("200과 함께 대화 상세를 반환한다") {
                    val message =
                        ChatMessageResult(
                            id = MESSAGE_ID,
                            role = ChatMessageRole.USER,
                            content = "오늘 하루 어때요?",
                            status = ChatMessageStatus.COMPLETED,
                            action = null,
                            createdAt = Instant.parse("2026-08-16T00:00:00Z"),
                        )
                    val detail = ConversationDetailResult(id = CONVERSATION_ID, title = "오늘 하루 어때요?", messages = listOf(message))
                    every { openConversationUseCase.open(MEMBER_ID, CONVERSATION_ID) } returns detail

                    val data =
                        successData(
                            mockMvc.get("/api/v1/chat/conversations/$CONVERSATION_ID") { with(authenticatedMember()) },
                        )

                    data["id"].asString() shouldBe CONVERSATION_ID.toString()
                    data["messages"][0]["id"].asString() shouldBe MESSAGE_ID.toString()
                    data["messages"][0]["role"].asString() shouldBe "USER"
                    verify(exactly = 1) { openConversationUseCase.open(MEMBER_ID, CONVERSATION_ID) }
                }
            }

            context("존재하지 않는 대화를 조회하면") {
                it("404를 반환한다") {
                    every { openConversationUseCase.open(MEMBER_ID, CONVERSATION_ID) } throws ChatConversationNotFoundException()

                    mockMvc
                        .get("/api/v1/chat/conversations/$CONVERSATION_ID") { with(authenticatedMember()) }
                        .andExpect { status { isNotFound() } }
                }
            }

            context("본인 소유가 아닌 대화를 조회하면") {
                it("403을 반환한다") {
                    every { openConversationUseCase.open(MEMBER_ID, CONVERSATION_ID) } throws ChatConversationForbiddenException()

                    mockMvc
                        .get("/api/v1/chat/conversations/$CONVERSATION_ID") { with(authenticatedMember()) }
                        .andExpect { status { isForbidden() } }
                }
            }
        }

        describe("DELETE /api/v1/chat/conversations/{conversationId}") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.delete("/api/v1/chat/conversations/$CONVERSATION_ID")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { deleteConversationUseCase.delete(any(), any()) }
                }
            }

            context("인증된 회원이 본인 소유 대화를 삭제하면") {
                it("200을 반환하고 삭제 유스케이스를 호출한다") {
                    every { deleteConversationUseCase.delete(MEMBER_ID, CONVERSATION_ID) } just Runs

                    mockMvc
                        .delete("/api/v1/chat/conversations/$CONVERSATION_ID") { with(authenticatedMember()) }
                        .andExpect { status { isOk() } }

                    verify(exactly = 1) { deleteConversationUseCase.delete(MEMBER_ID, CONVERSATION_ID) }
                }
            }

            context("본인 소유가 아닌 대화를 삭제하면") {
                it("403을 반환한다") {
                    every {
                        deleteConversationUseCase.delete(MEMBER_ID, CONVERSATION_ID)
                    } throws ChatConversationForbiddenException()

                    mockMvc
                        .delete("/api/v1/chat/conversations/$CONVERSATION_ID") { with(authenticatedMember()) }
                        .andExpect { status { isForbidden() } }
                }
            }
        }
    }

    private fun authenticatedMember() = authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList()))

    private fun successData(result: ResultActionsDsl): JsonNode =
        result
            .andExpect { status { isOk() } }
            .andReturn()
            .response.contentAsString
            .let(objectMapper::readTree)
            .also { it["success"].asBoolean() shouldBe true }["data"]
}
