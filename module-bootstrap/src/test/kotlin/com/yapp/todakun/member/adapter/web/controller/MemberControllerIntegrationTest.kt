package com.yapp.todakun.member.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.Member
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.Role
import com.yapp.todakun.member.port.inbound.GetMyProfileUseCase
import com.yapp.todakun.member.port.inbound.UpdateMemberUseCase
import com.yapp.todakun.member.port.inbound.WithdrawCommand
import com.yapp.todakun.member.port.inbound.WithdrawUseCase
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class MemberControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getMyProfileUseCase: GetMyProfileUseCase

    @MockkBean
    private lateinit var updateMemberUseCase: UpdateMemberUseCase

    @MockkBean
    private lateinit var withdrawUseCase: WithdrawUseCase

    init {
        afterTest { clearMocks(getMyProfileUseCase, updateMemberUseCase, withdrawUseCase) }

        fun member(): Member =
            Member.reconstitute(
                id = MEMBER_ID,
                name = "토닥이",
                birthDate = LocalDate.of(1999, 2, 13),
                birthTime = BirthTime.JASI,
                calendarType = CalendarType.SOLAR,
                gender = Gender.FEMALE,
                role = Role.MEMBER,
                oauthProvider = OauthProvider.GOOGLE,
                providerId = "1234567890",
                job = Job.WORKER,
                relationshipStatus = RelationshipStatus.SOLO,
                favoriteFortuneCategories = setOf(FortuneCategory.RELATIONSHIP, FortuneCategory.ACHIEVEMENT),
            )

        describe("GET /api/v1/members/me") {
            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized)
                }
            }

            context("인증된 회원이 요청하면") {
                it("본인 프로필(관심 주제 포함)을 반환한다") {
                    every { getMyProfileUseCase.getProfile(MEMBER_ID) } returns member()

                    val response =
                        mockMvc
                            .perform(
                                get("/api/v1/members/me")
                                    .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList()))),
                            ).andExpect(status().isOk)
                            .andReturn()
                            .response.contentAsString

                    val body = objectMapper.readTree(response)
                    body["data"]["name"].asString() shouldBe "토닥이"
                    body["data"]["favoriteFortuneCategories"].size() shouldBe 2
                }
            }
        }

        describe("PATCH /api/v1/members/me") {
            val requestBody =
                mapOf(
                    "gender" to "FEMALE",
                    "calendarType" to "SOLAR",
                    "birthDate" to "1999-02-13",
                    "birthTime" to "JASI",
                    "job" to "WORKER",
                    "relationshipStatus" to "DATING",
                    "favoriteFortuneCategories" to listOf("LOVE", "MONEY"),
                )

            context("인증된 회원이 유효한 값으로 요청하면") {
                it("200을 반환하고 정보를 수정한다") {
                    every { updateMemberUseCase.update(any()) } just Runs

                    mockMvc
                        .perform(
                            patch("/api/v1/members/me")
                                .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList())))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(requestBody)),
                        ).andExpect(status().isOk)

                    verify(exactly = 1) { updateMemberUseCase.update(any()) }
                }
            }

            context("관심 주제가 비어 있으면") {
                it("400을 반환한다") {
                    mockMvc
                        .perform(
                            patch("/api/v1/members/me")
                                .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList())))
                                .contentType("application/json")
                                .content(
                                    objectMapper.writeValueAsString(requestBody + ("favoriteFortuneCategories" to emptyList<String>())),
                                ),
                        ).andExpect(status().isBadRequest)
                }
            }
        }

        describe("DELETE /api/v1/members/me") {
            context("인증된 회원이 탈퇴 사유와 함께 요청하면") {
                it("200을 반환하고 사유로 탈퇴 처리한다") {
                    val commandSlot = slot<WithdrawCommand>()
                    every { withdrawUseCase.withdraw(capture(commandSlot)) } just Runs

                    mockMvc
                        .perform(
                            delete("/api/v1/members/me")
                                .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList())))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(mapOf("reason" to "NOT_USING"))),
                        ).andExpect(status().isOk)

                    commandSlot.captured.memberId shouldBe MEMBER_ID
                    commandSlot.captured.reason.name shouldBe "NOT_USING"
                }
            }

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc
                        .perform(
                            delete("/api/v1/members/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(mapOf("reason" to "NOT_USING"))),
                        ).andExpect(status().isUnauthorized)
                }
            }
        }
    }
}
