package com.yapp.todakun.auth.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.web.dto.request.LoginRequest
import com.yapp.todakun.auth.adapter.web.dto.request.RefreshRequest
import com.yapp.todakun.auth.adapter.web.dto.request.SignupRequest
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID

// 로그인/가입 흐름 자체를 검증할 뿐 값 자체는 검증 대상이 아닌 필드들(유효성만 통과하면 되는 기본값)
private const val NAME = "홍길동"

// 존재/형식 여부만 중요하고 값 자체는 의미가 없는 온보딩 토큰(조회 실패·검증 실패 유도용)
private const val INVALID_ONBOARDING_TOKEN = "invalid-onboarding-token"

// 존재/형식 여부만 중요하고 값 자체는 의미가 없는 액세스 토큰(파싱 실패 유도용)
private const val INVALID_ACCESS_TOKEN = "invalid-access-token"

// 존재/형식 여부만 중요하고 값 자체는 의미가 없는 refresh 토큰(조회 실패 유도용)
private const val INVALID_REFRESH_TOKEN = "invalid-refresh-token"

private const val AUTHORIZATION_CODE = "test-authorization-code"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class AuthControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val dailyFortuneAiPort: DailyFortuneAiPort,
) : DescribeSpec() {
    @MockkBean
    private lateinit var oauthPort: OauthPort

    init {
        beforeTest { every { dailyFortuneAiPort.generate(any(), any(), any()) } returns FAKE_GENERATED_DAILY_FORTUNE }
        afterTest { clearMocks(oauthPort, dailyFortuneAiPort) }

        describe("POST /api/v1/auth/login") {
            context("신규 회원이면") {
                it("온보딩 토큰을 발급한다") {
                    val data = login(stubNewOauthProfile())

                    data["isNewMember"].asBoolean() shouldBe true
                    data["onboardingToken"].asString().shouldNotBeBlank()
                    data["accessToken"].isNull shouldBe true
                    data["refreshToken"].isNull shouldBe true
                }
            }

            context("기존 회원이면") {
                it("access/refresh 토큰을 발급한다") {
                    val oauthAccessToken = stubNewOauthProfile()
                    val onboardingToken = login(oauthAccessToken)["onboardingToken"].asString()
                    signup(onboardingToken).andExpect { status { isCreated() } }

                    val data = login(oauthAccessToken)

                    data["isNewMember"].asBoolean() shouldBe false
                    data["accessToken"].asString().shouldNotBeBlank()
                    data["refreshToken"].asString().shouldNotBeBlank()
                    data["onboardingToken"].isNull shouldBe true
                }
            }

            context("요청 바디가 유효하지 않으면") {
                it("400을 반환한다") {
                    mockMvc
                        .post("/api/v1/auth/login") {
                            contentType = MediaType.APPLICATION_JSON
                            content = LoginRequest(provider = OauthProvider.KAKAO, oauthAccessToken = "").toJson()
                        }.andExpect { status { isBadRequest() } }
                }
            }

            context("provider가 APPLE이고 authorizationCode가 있으면") {
                it("OauthPort.fetchProfile에 authorizationCode를 전달한다") {
                    val authorizationCode = AUTHORIZATION_CODE
                    val oauthAccessToken = stubNewOauthProfile(provider = OauthProvider.APPLE, authorizationCode = authorizationCode)

                    val data = login(oauthAccessToken, provider = OauthProvider.APPLE, authorizationCode = authorizationCode)

                    data["isNewMember"].asBoolean() shouldBe true
                    verify(exactly = 1) { oauthPort.fetchProfile(OauthProvider.APPLE, oauthAccessToken, authorizationCode) }
                }
            }
        }

        describe("POST /api/v1/auth/signup") {
            context("인증 헤더 없이 요청해도") {
                it("정상 처리된다") {
                    val onboardingToken = login(stubNewOauthProfile())["onboardingToken"].asString()

                    signup(onboardingToken).andExpect { status { isCreated() } }
                }
            }

            context("존재하지 않는 온보딩 토큰이면") {
                it("401을 반환한다") {
                    signup(onboardingToken = INVALID_ONBOARDING_TOKEN)
                        .andExpect { status { isUnauthorized() } }
                }
            }

            context("필수 필드가 누락되면") {
                it("400과 함께 실패한 필드별 사유를 reason에 담아 반환한다") {
                    val response =
                        signup(onboardingToken = INVALID_ONBOARDING_TOKEN, name = "")
                            .andExpect { status { isBadRequest() } }
                            .andReturn()
                            .response

                    val body = objectMapper.readTree(response.contentAsString)

                    body["success"].asBoolean() shouldBe false
                    body["code"].asString().shouldNotBeBlank()
                    body["reason"]["name"].asString().shouldNotBeBlank()
                }
            }
        }

        describe("POST /api/v1/auth/logout") {
            context("유효한 accessToken이 주어지면") {
                it("로그아웃에 성공하고, 이후 같은 토큰은 사용할 수 없다") {
                    val accessToken = issueAccessToken()

                    logout(accessToken).andExpect { status { isOk() } }

                    logout(accessToken).andExpect { status { isUnauthorized() } }
                }
            }

            context("인증 헤더 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.post("/api/v1/auth/logout")
                        .andExpect { status { isUnauthorized() } }
                }
            }

            context("형식이 올바르지 않은 토큰이면") {
                it("401을 반환한다") {
                    logout(INVALID_ACCESS_TOKEN).andExpect { status { isUnauthorized() } }
                }
            }
        }

        describe("POST /api/v1/auth/refresh") {
            context("유효한 refreshToken이 주어지면") {
                it("access/refresh 토큰을 재발급하고, 기존 refreshToken은 더 이상 사용할 수 없다") {
                    val refreshToken = issueRefreshToken()

                    val response = refresh(refreshToken).andExpect { status { isOk() } }.andReturn().response
                    val data = objectMapper.readTree(response.contentAsString)["data"]

                    data["accessToken"].asString().shouldNotBeBlank()
                    data["refreshToken"].asString().shouldNotBeBlank()
                    refresh(refreshToken).andExpect { status { isUnauthorized() } }
                }
            }

            context("존재하지 않는 refreshToken이면") {
                it("401을 반환한다") {
                    refresh(INVALID_REFRESH_TOKEN).andExpect { status { isUnauthorized() } }
                }
            }
        }
    }

    private fun login(
        oauthAccessToken: String,
        provider: OauthProvider = OauthProvider.KAKAO,
        authorizationCode: String? = null,
    ): JsonNode =
        mockMvc
            .post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    LoginRequest(
                        provider = provider,
                        oauthAccessToken = oauthAccessToken,
                        authorizationCode = authorizationCode,
                    ).toJson()
            }.andExpect { status { isOk() } }
            .andReturn()
            .response
            .let { objectMapper.readTree(it.contentAsString)["data"] }

    private fun signup(
        onboardingToken: String,
        name: String = NAME,
    ): ResultActionsDsl =
        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content =
                SignupRequest(
                    onboardingToken = onboardingToken,
                    name = name,
                    birthDate = LocalDate.of(2000, 1, 1),
                    birthTime = BirthTime.MYOSI.name,
                    calendarType = CalendarType.SOLAR.name,
                    gender = Gender.MALE.name,
                    job = Job.STUDENT.name,
                    relationshipStatus = RelationshipStatus.SOLO.name,
                ).toJson()
        }

    private fun logout(accessToken: String): ResultActionsDsl =
        mockMvc.post("/api/v1/auth/logout") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        }

    private fun refresh(refreshToken: String): ResultActionsDsl =
        mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = RefreshRequest(refreshToken = refreshToken).toJson()
        }

    /** 매 호출마다 고유한 OAuth 프로필을 스텁으로 등록하고, 그 프로필로 로그인할 때 사용할 oauthAccessToken을 반환한다. */
    private fun stubNewOauthProfile(
        provider: OauthProvider = OauthProvider.KAKAO,
        authorizationCode: String? = null,
    ): String {
        val oauthAccessToken = "oauth-token-${UUID.randomUUID()}"
        val providerId = "provider-${UUID.randomUUID()}"
        val profile = OauthMemberProfile(provider = provider, providerId = providerId, email = "$providerId@todakun.com")
        every { oauthPort.fetchProfile(provider, oauthAccessToken, authorizationCode) } returns profile

        return oauthAccessToken
    }

    /** 신규 OAuth 프로필로 회원가입까지 마친 뒤, 로그인해서 발급받은 accessToken을 반환한다. */
    private fun issueAccessToken(): String {
        val oauthAccessToken = stubNewOauthProfile()
        val onboardingToken = login(oauthAccessToken)["onboardingToken"].asString()
        signup(onboardingToken).andExpect { status { isCreated() } }

        return login(oauthAccessToken)["accessToken"].asString()
    }

    /** 신규 OAuth 프로필로 회원가입까지 마친 뒤, 로그인해서 발급받은 refreshToken을 반환한다. */
    private fun issueRefreshToken(): String {
        val oauthAccessToken = stubNewOauthProfile()
        val onboardingToken = login(oauthAccessToken)["onboardingToken"].asString()
        signup(onboardingToken).andExpect { status { isCreated() } }

        return login(oauthAccessToken)["refreshToken"].asString()
    }

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}

/** 회원가입 시 자동 생성되는 오늘의 운세용 AI 응답 스텁(내용 자체는 이 테스트의 검증 대상이 아니다). */
private val FAKE_GENERATED_DAILY_FORTUNE =
    GeneratedDailyFortune(
        title = "오늘은 활기찬 하루가 될 거예요",
        content = "전반적으로 운이 상승하는 하루입니다.",
        luckyItems = listOf("노란색", "마스크", "운동화", "셔츠", "안경"),
        cautionaryItems = listOf("검정색", "체크무늬", "라면", "시계", "우산"),
        categoryFortunes =
            FortuneCategory.entries.map {
                GeneratedCategoryFortune(
                    fortuneCategory = it,
                    score = 70,
                    title = "오늘의 액션",
                    content = "상세 해석",
                )
            },
    )
