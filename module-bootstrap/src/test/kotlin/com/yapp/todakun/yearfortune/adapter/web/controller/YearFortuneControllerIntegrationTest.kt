package com.yapp.todakun.yearfortune.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneNotFoundException
import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import com.yapp.todakun.yearfortune.port.inbound.GetYearSelectionFortuneUseCase
import com.yapp.todakun.yearfortune.port.inbound.YearSelectionFortuneDetail
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
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

private val YEAR_SELECTION_FORTUNE = YearSelectionFortuneFixture.create()

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class YearFortuneControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getYearSelectionFortuneUseCase: GetYearSelectionFortuneUseCase

    init {
        afterTest { clearMocks(getYearSelectionFortuneUseCase) }

        describe("GET /api/v1/year-fortunes/{year}") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/year-fortunes/${YEAR_SELECTION_FORTUNE.year}")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getYearSelectionFortuneUseCase.getByYear(any(), any()) }
                }
            }

            context("인증된 회원의 연도별 운세가 있으면") {
                it("200과 함께 연도별 운세 상세를 반환한다") {
                    val detail = YearSelectionFortuneDetail.from(YEAR_SELECTION_FORTUNE)
                    every {
                        getYearSelectionFortuneUseCase.getByYear(YEAR_SELECTION_FORTUNE.year, YEAR_SELECTION_FORTUNE.memberId)
                    } returns detail

                    val data =
                        successData(
                            mockMvc.get("/api/v1/year-fortunes/${YEAR_SELECTION_FORTUNE.year}") { with(authenticatedMember()) },
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
                    ) { getYearSelectionFortuneUseCase.getByYear(YEAR_SELECTION_FORTUNE.year, YEAR_SELECTION_FORTUNE.memberId) }
                }
            }

            context("인증된 회원의 해당 연도별 운세가 존재하지 않으면") {
                it("404를 반환한다") {
                    every {
                        getYearSelectionFortuneUseCase.getByYear(YEAR_SELECTION_FORTUNE.year, YEAR_SELECTION_FORTUNE.memberId)
                    } throws YearSelectionFortuneNotFoundException()

                    mockMvc
                        .get("/api/v1/year-fortunes/${YEAR_SELECTION_FORTUNE.year}") { with(authenticatedMember()) }
                        .andExpect { status { isNotFound() } }
                }
            }
        }
    }

    private fun authenticatedMember() =
        authentication(UsernamePasswordAuthenticationToken(YEAR_SELECTION_FORTUNE.memberId, null, emptyList()))

    private fun successData(result: ResultActionsDsl): JsonNode =
        result
            .andExpect { status { isOk() } }
            .andReturn()
            .response.contentAsString
            .let(objectMapper::readTree)
            .also { it["success"].asBoolean() shouldBe true }["data"]
}
