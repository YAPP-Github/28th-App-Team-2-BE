package com.yapp.todakun.auth.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.adapter.web.dto.request.LoginRequest
import com.yapp.todakun.auth.adapter.web.dto.request.SignupRequest
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.clearMocks
import io.mockk.every
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class AuthControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var oauthPort: OauthPort

    init {
        afterTest { clearMocks(oauthPort) }

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
                it("400을 반환한다") {
                    signup(onboardingToken = INVALID_ONBOARDING_TOKEN, name = "")
                        .andExpect { status { isBadRequest() } }
                }
            }
        }
    }

    /** 매 호출마다 고유한 OAuth 프로필을 스텁으로 등록하고, 그 프로필로 로그인할 때 사용할 oauthAccessToken을 반환한다. */
    private fun stubNewOauthProfile(provider: OauthProvider = OauthProvider.KAKAO): String {
        val oauthAccessToken = "oauth-token-${UUID.randomUUID()}"
        val providerId = "provider-${UUID.randomUUID()}"
        val profile = OauthMemberProfile(provider = provider, providerId = providerId, email = "$providerId@todakun.com")
        every { oauthPort.fetchProfile(provider, oauthAccessToken) } returns profile

        return oauthAccessToken
    }

    private fun login(
        oauthAccessToken: String,
        provider: OauthProvider = OauthProvider.KAKAO,
    ): JsonNode =
        mockMvc
            .post("/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = LoginRequest(provider = provider, oauthAccessToken = oauthAccessToken).toJson()
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

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}
