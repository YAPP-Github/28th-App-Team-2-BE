package com.yapp.todakun.fortune.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.fortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.fortune.fixture.DailyFortuneFixture
import com.yapp.todakun.fortune.port.inbound.FortuneDetail
import com.yapp.todakun.fortune.port.inbound.GetFortuneUseCase
import com.yapp.todakun.fortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.fortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.LuckActionScore
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
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
import java.util.UUID

private val DAILY_FORTUNE = DailyFortuneFixture.create()
private val LUCK_ACTION_SCORES =
    listOf(
        LuckActionScore(
            id = UUID.fromString("018f0000-0000-7000-8000-000000000004"),
            fortuneCategory = FortuneCategory.HEALTH,
            score = 80,
        ),
        LuckActionScore(
            id = UUID.fromString("018f0000-0000-7000-8000-000000000005"),
            fortuneCategory = FortuneCategory.MONEY,
            score = 70,
        ),
        LuckActionScore(
            id = UUID.fromString("018f0000-0000-7000-8000-000000000006"),
            fortuneCategory = FortuneCategory.LOVE,
            score = 90,
        ),
    )

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class FortuneControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getTodayFortuneUseCase: GetTodayFortuneUseCase

    @MockkBean
    private lateinit var getFortuneUseCase: GetFortuneUseCase

    init {
        afterTest { clearMocks(getTodayFortuneUseCase, getFortuneUseCase) }

        describe("GET /api/v1/fortunes/today") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/fortunes/today")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getTodayFortuneUseCase.getToday(any(), any()) }
                }
            }

            context("인증된 회원의 오늘의 운세가 있으면") {
                it("200과 함께 오늘의 운세 요약을 반환한다") {
                    val summary = TodayFortuneSummary.from(DAILY_FORTUNE, LUCK_ACTION_SCORES)
                    every { getTodayFortuneUseCase.getToday(DAILY_FORTUNE.memberId, any()) } returns summary

                    val data = successData(mockMvc.get("/api/v1/fortunes/today") { with(authenticatedMember()) })

                    data["id"].asString() shouldBe DAILY_FORTUNE.id.toString()
                    data["fortuneDate"].asString() shouldBe DAILY_FORTUNE.fortuneDate.toString()
                    data["score"].asInt() shouldBe DAILY_FORTUNE.score
                    data["title"].asString() shouldBe DAILY_FORTUNE.title
                    data["luckActionScores"][0]["fortuneCategory"].asString() shouldBe FortuneCategory.HEALTH.name
                    verify(exactly = 1) { getTodayFortuneUseCase.getToday(DAILY_FORTUNE.memberId, any()) }
                }
            }

            context("인증된 회원의 오늘의 운세가 아직 생성되지 않았으면") {
                it("200과 함께 data 없는 응답을 반환한다") {
                    every { getTodayFortuneUseCase.getToday(DAILY_FORTUNE.memberId, any()) } returns null

                    val response =
                        mockMvc.get("/api/v1/fortunes/today") { with(authenticatedMember()) }
                            .andExpect { status { isOk() } }
                            .andReturn()
                            .response.contentAsString
                            .let(objectMapper::readTree)

                    response["data"].shouldBeNull()
                }
            }
        }

        describe("GET /api/v1/fortunes/{fortuneId}") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/fortunes/${DAILY_FORTUNE.id}")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getFortuneUseCase.getById(any(), any()) }
                }
            }

            context("인증된 회원이 존재하는 id로 조회하면") {
                it("200과 함께 오늘의 운세 상세를 반환한다") {
                    val detail = FortuneDetail.from(DAILY_FORTUNE, LUCK_ACTION_SCORES)
                    every { getFortuneUseCase.getById(DAILY_FORTUNE.id, DAILY_FORTUNE.memberId) } returns detail

                    val data = successData(mockMvc.get("/api/v1/fortunes/${DAILY_FORTUNE.id}") { with(authenticatedMember()) })

                    data["id"].asString() shouldBe DAILY_FORTUNE.id.toString()
                    data["fortuneDate"].asString() shouldBe DAILY_FORTUNE.fortuneDate.toString()
                    data["content"].asString() shouldBe DAILY_FORTUNE.content
                    data["luckyItems"][0].asString() shouldBe DAILY_FORTUNE.luckyItems[0]
                    data["cautionaryItems"][0].asString() shouldBe DAILY_FORTUNE.cautionaryItems[0]
                    verify(exactly = 1) { getFortuneUseCase.getById(DAILY_FORTUNE.id, DAILY_FORTUNE.memberId) }
                }
            }

            context("존재하지 않는 id로 조회하면") {
                it("404를 반환한다") {
                    every { getFortuneUseCase.getById(DAILY_FORTUNE.id, DAILY_FORTUNE.memberId) } throws DailyFortuneNotFoundException()

                    mockMvc
                        .get("/api/v1/fortunes/${DAILY_FORTUNE.id}") { with(authenticatedMember()) }
                        .andExpect { status { isNotFound() } }
                }
            }
        }
    }

    private fun authenticatedMember() = authentication(UsernamePasswordAuthenticationToken(DAILY_FORTUNE.memberId, null, emptyList()))

    private fun successData(result: ResultActionsDsl): JsonNode =
        result
            .andExpect { status { isOk() } }
            .andReturn()
            .response.contentAsString
            .let(objectMapper::readTree)
            .also { it["success"].asBoolean() shouldBe true }["data"]
}
