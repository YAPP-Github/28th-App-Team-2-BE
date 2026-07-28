package com.yapp.todakun.luck.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.luck.port.inbound.GetLuckActionUseCase
import com.yapp.todakun.luck.port.inbound.GetLuckActionsUseCase
import com.yapp.todakun.luck.port.inbound.ToggleLuckActionUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

private val LUCK_ACTION = LuckActionFixture.create()

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class LuckActionControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getLuckActionUseCase: GetLuckActionUseCase

    @MockkBean
    private lateinit var getLuckActionsUseCase: GetLuckActionsUseCase

    @MockkBean
    private lateinit var toggleLuckActionUseCase: ToggleLuckActionUseCase

    init {
        afterTest { clearMocks(getLuckActionUseCase, getLuckActionsUseCase, toggleLuckActionUseCase) }

        describe("GET /api/v1/luck-actions/{luckActionId}") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/luck-actions/${LUCK_ACTION.id}")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getLuckActionUseCase.getById(any(), any()) }
                }
            }

            context("인증된 회원이 존재하는 id로 조회하면") {
                it("200과 함께 행운 액션을 반환한다") {
                    every { getLuckActionUseCase.getById(LUCK_ACTION.id, LUCK_ACTION.memberId) } returns LUCK_ACTION

                    val data = successData(mockMvc.get("/api/v1/luck-actions/${LUCK_ACTION.id}") { with(authenticatedMember()) })

                    data["id"].asString() shouldBe LUCK_ACTION.id.toString()
                    data["fortuneCategory"].asString() shouldBe LUCK_ACTION.fortuneCategory.name
                    data["score"].asInt() shouldBe LUCK_ACTION.score
                    data["title"].asString() shouldBe LUCK_ACTION.title
                    data["content"].asString() shouldBe LUCK_ACTION.content
                    data["achieved"].asBoolean() shouldBe LUCK_ACTION.achieved
                    verify(exactly = 1) { getLuckActionUseCase.getById(LUCK_ACTION.id, LUCK_ACTION.memberId) }
                }
            }

            context("존재하지 않는 id로 조회하면") {
                it("404를 반환한다") {
                    every { getLuckActionUseCase.getById(LUCK_ACTION.id, LUCK_ACTION.memberId) } throws LuckActionNotFoundException()

                    mockMvc
                        .get("/api/v1/luck-actions/${LUCK_ACTION.id}") { with(authenticatedMember()) }
                        .andExpect { status { isNotFound() } }
                }
            }
        }

        describe("GET /api/v1/luck-actions/today") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/luck-actions/today")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getLuckActionsUseCase.getTodayLuckActions(any(), any()) }
                }
            }

            context("인증된 회원이 요청하면") {
                it("200과 함께 오늘자 행운 액션 목록을 반환한다") {
                    every { getLuckActionsUseCase.getTodayLuckActions(LUCK_ACTION.memberId, any<LocalDate>()) } returns listOf(LUCK_ACTION)

                    val data = successData(mockMvc.get("/api/v1/luck-actions/today") { with(authenticatedMember()) })

                    data[0]["id"].asString() shouldBe LUCK_ACTION.id.toString()
                    data[0]["fortuneCategory"].asString() shouldBe LUCK_ACTION.fortuneCategory.name
                    data[0]["score"].asInt() shouldBe LUCK_ACTION.score
                    data[0]["title"].asString() shouldBe LUCK_ACTION.title
                    data[0]["achieved"].asBoolean() shouldBe LUCK_ACTION.achieved
                    data[0].has("content") shouldBe false
                    verify(exactly = 1) { getLuckActionsUseCase.getTodayLuckActions(LUCK_ACTION.memberId, any<LocalDate>()) }
                }
            }
        }

        describe("PATCH /api/v1/luck-actions/{luckActionId}/achievement") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.patch("/api/v1/luck-actions/${LUCK_ACTION.id}/achievement")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { toggleLuckActionUseCase.toggle(any(), any()) }
                }
            }

            context("인증된 회원이 요청하면") {
                it("200과 함께 토글된 달성 여부를 반환한다") {
                    val toggled = LUCK_ACTION.toggle()
                    every { toggleLuckActionUseCase.toggle(LUCK_ACTION.id, LUCK_ACTION.memberId) } returns toggled

                    val data =
                        successData(
                            mockMvc.patch("/api/v1/luck-actions/${LUCK_ACTION.id}/achievement") { with(authenticatedMember()) },
                        )

                    data["id"].asString() shouldBe toggled.id.toString()
                    data["fortuneCategory"].asString() shouldBe toggled.fortuneCategory.name
                    data["score"].asInt() shouldBe toggled.score
                    data["title"].asString() shouldBe toggled.title
                    data["content"].asString() shouldBe toggled.content
                    data["achieved"].asBoolean() shouldBe toggled.achieved
                    verify(exactly = 1) { toggleLuckActionUseCase.toggle(LUCK_ACTION.id, LUCK_ACTION.memberId) }
                }
            }

            context("존재하지 않는 id로 토글하면") {
                it("404를 반환한다") {
                    every { toggleLuckActionUseCase.toggle(LUCK_ACTION.id, LUCK_ACTION.memberId) } throws LuckActionNotFoundException()

                    mockMvc
                        .patch("/api/v1/luck-actions/${LUCK_ACTION.id}/achievement") { with(authenticatedMember()) }
                        .andExpect { status { isNotFound() } }
                }
            }
        }
    }

    private fun authenticatedMember() = authentication(UsernamePasswordAuthenticationToken(LUCK_ACTION.memberId, null, emptyList()))

    private fun successData(result: ResultActionsDsl): JsonNode =
        result
            .andExpect { status { isOk() } }
            .andReturn()
            .response.contentAsString
            .let(objectMapper::readTree)
            .also { it["success"].asBoolean() shouldBe true }["data"]
}
