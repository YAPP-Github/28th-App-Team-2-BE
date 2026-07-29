package com.yapp.todakun.yearfortune.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import com.yapp.todakun.yearfortune.port.inbound.CreateYearSelectionFortuneUseCase
import com.yapp.todakun.yearfortune.port.inbound.YearSelectionFortuneResult
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
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

private val YEAR_SELECTION_FORTUNE = YearSelectionFortuneFixture.create()

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class YearFortuneControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var createYearSelectionFortuneUseCase: CreateYearSelectionFortuneUseCase

    init {
        afterTest { clearMocks(createYearSelectionFortuneUseCase) }

        describe("POST /api/v1/year-fortunes/{year}") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.post("/api/v1/year-fortunes/${YEAR_SELECTION_FORTUNE.year}")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { createYearSelectionFortuneUseCase.create(any(), any()) }
                }
            }

            context("인증된 회원이 요청하면") {
                it("201과 함께 연도별 운세를 반환한다") {
                    val result = YearSelectionFortuneResult.from(YEAR_SELECTION_FORTUNE)
                    every {
                        createYearSelectionFortuneUseCase.create(YEAR_SELECTION_FORTUNE.year, YEAR_SELECTION_FORTUNE.memberId)
                    } returns result

                    val data =
                        successData(
                            mockMvc.post("/api/v1/year-fortunes/${YEAR_SELECTION_FORTUNE.year}") { with(authenticatedMember()) },
                        )

                    data["id"].asString() shouldBe YEAR_SELECTION_FORTUNE.id.toString()
                    data["year"].asInt() shouldBe YEAR_SELECTION_FORTUNE.year
                    data["score"].asInt() shouldBe YEAR_SELECTION_FORTUNE.score
                    data["title"].asString() shouldBe YEAR_SELECTION_FORTUNE.title
                    data["content"].asString() shouldBe YEAR_SELECTION_FORTUNE.content
                    data["fortuneCategories"][0]["fortuneCategory"].asString() shouldBe FortuneCategory.RELATIONSHIP.name
                    data["fortuneCategories"][0]["star"].asInt() shouldBe 2
                    verify(
                        exactly = 1,
                    ) { createYearSelectionFortuneUseCase.create(YEAR_SELECTION_FORTUNE.year, YEAR_SELECTION_FORTUNE.memberId) }
                }
            }
        }
    }

    private fun authenticatedMember() =
        authentication(UsernamePasswordAuthenticationToken(YEAR_SELECTION_FORTUNE.memberId, null, emptyList()))

    private fun successData(result: ResultActionsDsl): JsonNode =
        result
            .andExpect { status { isCreated() } }
            .andReturn()
            .response.contentAsString
            .let(objectMapper::readTree)
            .also { it["success"].asBoolean() shouldBe true }["data"]
}
