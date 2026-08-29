package com.yapp.todakun.dailyfortune.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.dailyfortune.exception.DailyFortuneBatchAlreadyRunningException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneHistoryToOutOfRangeException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import com.yapp.todakun.dailyfortune.port.inbound.DailyFortuneDetail
import com.yapp.todakun.dailyfortune.port.inbound.DailyFortuneHistorySummary
import com.yapp.todakun.dailyfortune.port.inbound.GetDailyFortuneHistoryUseCase
import com.yapp.todakun.dailyfortune.port.inbound.GetDailyFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.RegenerateDailyFortunesUseCase
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneResult
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.LuckActionScore
import com.yapp.todakun.shared.LuckActionSummary
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
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
private val LUCK_ACTION_SUMMARIES =
    listOf(
        LuckActionSummary(
            id = UUID.fromString("018f0000-0000-7000-8000-000000000007"),
            fortuneDate = DAILY_FORTUNE.fortuneDate,
            fortuneCategory = FortuneCategory.HEALTH,
            title = "건강 챙기기",
            achieved = true,
        ),
    )
private val FORTUNE_HISTORY_SUMMARY =
    DailyFortuneHistorySummary(
        id = DAILY_FORTUNE.id,
        fortuneDate = DAILY_FORTUNE.fortuneDate,
        score = DAILY_FORTUNE.score,
        title = DAILY_FORTUNE.title,
        luckActions = LUCK_ACTION_SUMMARIES,
    )

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class DailyFortuneControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getTodayFortuneUseCase: GetTodayFortuneUseCase

    @MockkBean
    private lateinit var getFortuneUseCase: GetDailyFortuneUseCase

    @MockkBean
    private lateinit var getFortuneHistoryUseCase: GetDailyFortuneHistoryUseCase

    @MockkBean
    private lateinit var regenerateDailyFortunesUseCase: RegenerateDailyFortunesUseCase

    init {
        afterTest {
            clearMocks(getTodayFortuneUseCase, getFortuneUseCase, getFortuneHistoryUseCase, regenerateDailyFortunesUseCase)
        }

        describe("GET /api/v1/daily-fortunes/today") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/daily-fortunes/today")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getTodayFortuneUseCase.getToday(any(), any()) }
                }
            }

            context("인증된 회원의 오늘의 운세가 있으면") {
                it("200과 함께 완료 상태의 오늘의 운세 요약을 반환한다") {
                    val summary = TodayFortuneSummary.from(DAILY_FORTUNE, LUCK_ACTION_SCORES)
                    every { getTodayFortuneUseCase.getToday(DAILY_FORTUNE.memberId, any()) } returns TodayFortuneResult.completed(summary)

                    val data = successData(mockMvc.get("/api/v1/daily-fortunes/today") { with(authenticatedMember()) })

                    data["status"].asString() shouldBe "COMPLETED"
                    data["id"].asString() shouldBe DAILY_FORTUNE.id.toString()
                    data["fortuneDate"].asString() shouldBe DAILY_FORTUNE.fortuneDate.toString()
                    data["score"].asInt() shouldBe DAILY_FORTUNE.score
                    data["title"].asString() shouldBe DAILY_FORTUNE.title
                    data["luckActionScores"][0]["fortuneCategory"].asString() shouldBe FortuneCategory.HEALTH.name
                    verify(exactly = 1) { getTodayFortuneUseCase.getToday(DAILY_FORTUNE.memberId, any()) }
                }
            }

            context("다른 호출자가 이미 생성 중이면") {
                it("200과 함께 생성 중 상태만 반환하고 나머지 필드는 생략한다") {
                    every { getTodayFortuneUseCase.getToday(DAILY_FORTUNE.memberId, any()) } returns TodayFortuneResult.generating()

                    val data = successData(mockMvc.get("/api/v1/daily-fortunes/today") { with(authenticatedMember()) })

                    data["status"].asString() shouldBe "GENERATING"
                    data.has("id") shouldBe false
                }
            }

            context("인증된 회원의 오늘의 운세가 존재하지 않으면") {
                it("404를 반환한다") {
                    every { getTodayFortuneUseCase.getToday(DAILY_FORTUNE.memberId, any()) } throws DailyFortuneNotFoundException()

                    mockMvc.get("/api/v1/daily-fortunes/today") { with(authenticatedMember()) }
                        .andExpect { status { isNotFound() } }
                }
            }
        }

        describe("GET /api/v1/daily-fortunes/{dailyFortuneId}") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/daily-fortunes/${DAILY_FORTUNE.id}")
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getFortuneUseCase.getById(any(), any()) }
                }
            }

            context("인증된 회원이 존재하는 id로 조회하면") {
                it("200과 함께 오늘의 운세 상세를 반환한다") {
                    val detail = DailyFortuneDetail.from(DAILY_FORTUNE, LUCK_ACTION_SCORES)
                    every { getFortuneUseCase.getById(DAILY_FORTUNE.id, DAILY_FORTUNE.memberId) } returns detail

                    val data = successData(mockMvc.get("/api/v1/daily-fortunes/${DAILY_FORTUNE.id}") { with(authenticatedMember()) })

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
                        .get("/api/v1/daily-fortunes/${DAILY_FORTUNE.id}") { with(authenticatedMember()) }
                        .andExpect { status { isNotFound() } }
                }
            }
        }

        describe("GET /api/v1/daily-fortunes/history") {
            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.get("/api/v1/daily-fortunes/history") { param("to", DAILY_FORTUNE.fortuneDate.toString()) }
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { getFortuneHistoryUseCase.getHistory(any(), any(), any()) }
                }
            }

            context("인증된 회원이 요청하면") {
                it("200과 함께 히스토리 목록을 반환한다") {
                    every {
                        getFortuneHistoryUseCase.getHistory(DAILY_FORTUNE.memberId, any(), DAILY_FORTUNE.fortuneDate)
                    } returns listOf(FORTUNE_HISTORY_SUMMARY)

                    val data =
                        successData(
                            mockMvc.get("/api/v1/daily-fortunes/history") {
                                param("to", DAILY_FORTUNE.fortuneDate.toString())
                                with(authenticatedMember())
                            },
                        )

                    data[0]["id"].asString() shouldBe DAILY_FORTUNE.id.toString()
                    data[0]["fortuneDate"].asString() shouldBe DAILY_FORTUNE.fortuneDate.toString()
                    data[0]["score"].asInt() shouldBe DAILY_FORTUNE.score
                    data[0]["title"].asString() shouldBe DAILY_FORTUNE.title
                    data[0]["luckActions"][0]["fortuneCategory"].asString() shouldBe FortuneCategory.HEALTH.name
                    data[0]["luckActions"][0]["achieved"].asBoolean() shouldBe true
                    verify(exactly = 1) { getFortuneHistoryUseCase.getHistory(DAILY_FORTUNE.memberId, any(), DAILY_FORTUNE.fortuneDate) }
                }
            }

            context("to가 허용 범위를 벗어나면") {
                it("400을 반환한다") {
                    every { getFortuneHistoryUseCase.getHistory(any(), any(), any()) } throws DailyFortuneHistoryToOutOfRangeException()

                    mockMvc
                        .get("/api/v1/daily-fortunes/history") {
                            param("to", DAILY_FORTUNE.fortuneDate.toString())
                            with(authenticatedMember())
                        }
                        .andExpect { status { isBadRequest() } }
                }
            }
        }

        describe("POST /api/v1/daily-fortunes/regenerate") {
            beforeTest { every { regenerateDailyFortunesUseCase.regenerate(any()) } just runs }

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.post("/api/v1/daily-fortunes/regenerate") { param("fortuneDate", DAILY_FORTUNE.fortuneDate.toString()) }
                        .andExpect { status { isUnauthorized() } }

                    verify(exactly = 0) { regenerateDailyFortunesUseCase.regenerate(any()) }
                }
            }

            context("ROLE_ADMIN 권한이 없으면") {
                it("403을 반환한다") {
                    mockMvc.post("/api/v1/daily-fortunes/regenerate") {
                        param("fortuneDate", DAILY_FORTUNE.fortuneDate.toString())
                        with(authenticatedMember())
                    }.andExpect { status { isForbidden() } }

                    verify(exactly = 0) { regenerateDailyFortunesUseCase.regenerate(any()) }
                }
            }

            context("ROLE_ADMIN 권한으로 요청하면") {
                it("전달받은 날짜 기준 배치를 재실행하고 200을 반환한다") {
                    mockMvc.post("/api/v1/daily-fortunes/regenerate") {
                        param("fortuneDate", DAILY_FORTUNE.fortuneDate.toString())
                        with(authenticatedAdmin())
                    }.andExpect { status { isOk() } }

                    verify(exactly = 1) { regenerateDailyFortunesUseCase.regenerate(DAILY_FORTUNE.fortuneDate) }
                }
            }

            context("같은 날짜로 이미 실행 중인 배치가 있으면") {
                it("409를 반환한다") {
                    every { regenerateDailyFortunesUseCase.regenerate(any()) } throws DailyFortuneBatchAlreadyRunningException()

                    mockMvc.post("/api/v1/daily-fortunes/regenerate") {
                        param("fortuneDate", DAILY_FORTUNE.fortuneDate.toString())
                        with(authenticatedAdmin())
                    }.andExpect { status { isConflict() } }
                }
            }
        }
    }

    private fun authenticatedMember() = authentication(UsernamePasswordAuthenticationToken(DAILY_FORTUNE.memberId, null, emptyList()))

    private fun authenticatedAdmin() =
        authentication(
            UsernamePasswordAuthenticationToken(DAILY_FORTUNE.memberId, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN"))),
        )

    private fun successData(result: ResultActionsDsl): JsonNode =
        result
            .andExpect { status { isOk() } }
            .andReturn()
            .response.contentAsString
            .let(objectMapper::readTree)
            .also { it["success"].asBoolean() shouldBe true }["data"]
}
