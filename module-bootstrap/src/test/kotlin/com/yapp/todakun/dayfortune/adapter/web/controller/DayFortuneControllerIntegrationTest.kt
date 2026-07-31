package com.yapp.todakun.dayfortune.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.dayfortune.adapter.web.dto.request.CreateDaySelectionFortuneRequest
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import com.yapp.todakun.dayfortune.port.inbound.CreateDaySelectionFortuneUseCase
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import com.yapp.todakun.shared.FortuneCategory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

private val DAY_SELECTION_FORTUNE = DaySelectionFortuneFixture.create()

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class DayFortuneControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var createDaySelectionFortuneUseCase: CreateDaySelectionFortuneUseCase

    init {
        afterTest { clearMocks(createDaySelectionFortuneUseCase) }

        describe("POST /api/v1/day-fortunes") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    request(defaultRequestBody())
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { createDaySelectionFortuneUseCase.create(any(), any(), any()) }
                }
            }

            context("인증된 회원이 요청하면") {
                it("201과 함께 택일 운세 목록을 반환한다") {
                    val result = DaySelectionFortuneResult.from(DAY_SELECTION_FORTUNE)
                    every {
                        createDaySelectionFortuneUseCase.create(
                            DAY_SELECTION_FORTUNE.purpose,
                            listOf(DAY_SELECTION_FORTUNE.targetDate),
                            DAY_SELECTION_FORTUNE.memberId,
                        )
                    } returns listOf(result)

                    val data = successData(request(defaultRequestBody()) { with(authenticatedMember()) })

                    data[0]["id"].asString() shouldBe DAY_SELECTION_FORTUNE.id.toString()
                    data[0]["purpose"].asString() shouldBe DAY_SELECTION_FORTUNE.purpose.name
                    data[0]["targetDate"].asString() shouldBe DAY_SELECTION_FORTUNE.targetDate.toString()
                    data[0]["score"].asInt() shouldBe DAY_SELECTION_FORTUNE.score
                    data[0]["title"].asString() shouldBe DAY_SELECTION_FORTUNE.title
                    data[0]["content"].asString() shouldBe DAY_SELECTION_FORTUNE.content
                    data[0]["fortuneCategories"][0]["fortuneCategory"].asString() shouldBe FortuneCategory.RELATIONSHIP.name
                    data[0]["fortuneCategories"][0]["star"].asInt() shouldBe 2
                    verify(exactly = 1) {
                        createDaySelectionFortuneUseCase.create(
                            DAY_SELECTION_FORTUNE.purpose,
                            listOf(DAY_SELECTION_FORTUNE.targetDate),
                            DAY_SELECTION_FORTUNE.memberId,
                        )
                    }
                }
            }

            context("후보 날짜를 6개 이상 요청하면") {
                it("400을 반환한다") {
                    val invalidBody =
                        CreateDaySelectionFortuneRequest(
                            purpose = DAY_SELECTION_FORTUNE.purpose.name,
                            targetDates = List(6) { DAY_SELECTION_FORTUNE.targetDate.plusDays(it.toLong()) },
                        )

                    request(invalidBody) { with(authenticatedMember()) }
                        .andExpect { status { isBadRequest() } }
                }
            }
        }
    }

    private fun defaultRequestBody() =
        CreateDaySelectionFortuneRequest(
            purpose = DAY_SELECTION_FORTUNE.purpose.name,
            targetDates = listOf(DAY_SELECTION_FORTUNE.targetDate),
        )

    private fun request(
        body: CreateDaySelectionFortuneRequest,
        block: MockHttpServletRequestDsl.() -> Unit = {},
    ): ResultActionsDsl =
        mockMvc.post("/api/v1/day-fortunes") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
            block()
        }

    private fun authenticatedMember() =
        authentication(UsernamePasswordAuthenticationToken(DAY_SELECTION_FORTUNE.memberId, null, emptyList()))

    private fun successData(result: ResultActionsDsl): JsonNode =
        result
            .andExpect { status { isCreated() } }
            .andReturn()
            .response.contentAsString
            .let(objectMapper::readTree)
            .also { it["success"].asBoolean() shouldBe true }["data"]
}
