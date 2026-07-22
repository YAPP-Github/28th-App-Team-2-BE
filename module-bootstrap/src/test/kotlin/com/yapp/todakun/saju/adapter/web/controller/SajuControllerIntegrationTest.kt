package com.yapp.todakun.saju.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.port.inbound.DeletePartnerSajuUseCase
import com.yapp.todakun.saju.port.inbound.GetMySajuUseCase
import com.yapp.todakun.saju.port.inbound.GetPartnerSajuUseCase
import com.yapp.todakun.saju.port.inbound.GetPartnerSajusUseCase
import com.yapp.todakun.saju.port.inbound.RegisterPartnerSajuCommand
import com.yapp.todakun.saju.port.inbound.RegisterPartnerSajuUseCase
import com.yapp.todakun.saju.port.inbound.SajuChartDetail
import com.yapp.todakun.saju.port.inbound.UpdatePartnerSajuUseCase
import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.slot
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val PARTNER_SAJU_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000d1")

@ExperimentalUuidApi
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class SajuControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getMySajuUseCase: GetMySajuUseCase

    @MockkBean
    private lateinit var registerPartnerSajuUseCase: RegisterPartnerSajuUseCase

    @MockkBean
    private lateinit var getPartnerSajusUseCase: GetPartnerSajusUseCase

    @MockkBean
    private lateinit var getPartnerSajuUseCase: GetPartnerSajuUseCase

    @MockkBean
    private lateinit var updatePartnerSajuUseCase: UpdatePartnerSajuUseCase

    @MockkBean
    private lateinit var deletePartnerSajuUseCase: DeletePartnerSajuUseCase

    init {
        afterTest {
            clearMocks(
                getMySajuUseCase,
                registerPartnerSajuUseCase,
                getPartnerSajusUseCase,
                getPartnerSajuUseCase,
                updatePartnerSajuUseCase,
                deletePartnerSajuUseCase,
            )
        }

        fun sampleDetail(): SajuChartDetail {
            val chart =
                SajuChart.create(
                    name = "토닥이",
                    gender = Gender.FEMALE,
                    calendarType = CalendarType.SOLAR,
                    birthDate = LocalDate.of(2001, 5, 30),
                    birthTime = BirthTime.MISI,
                    isLeapMonth = false,
                    fourPillars =
                        FourPillars(
                            year = GanjiPillar(HeavenlyStem.SIN, EarthlyBranch.SA),
                            month = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
                            day = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
                            hour = GanjiPillar(HeavenlyStem.GI, EarthlyBranch.MI),
                            solarTermName = "입하",
                        ),
                )
            return SajuChartDetail.from(MemberSajuLink.self(MEMBER_ID, chart.id), chart)
        }

        describe("GET /api/v1/saju/me") {
            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc.perform(get("/api/v1/saju/me")).andExpect(status().isUnauthorized)
                }
            }

            context("인증된 회원이 요청하면") {
                it("본인 만세력 상세를 반환한다") {
                    every { getMySajuUseCase.getMine(MEMBER_ID) } returns sampleDetail()

                    val response =
                        mockMvc
                            .perform(
                                get("/api/v1/saju/me")
                                    .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList()))),
                            ).andExpect(status().isOk)
                            .andReturn()
                            .response.contentAsString

                    val body = objectMapper.readTree(response)
                    body["success"].asBoolean() shouldBe true
                    body["data"]["role"].asString() shouldBe "SELF"
                    body["data"]["pillars"].size() shouldBe 4
                }
            }
        }

        describe("POST /api/v1/saju/partners") {
            val requestBody =
                mapOf(
                    "name" to "토실이",
                    "gender" to "MALE",
                    "calendarType" to "SOLAR",
                    "birthDate" to "1999-02-13",
                    "birthTime" to "SINSI",
                    "relationshipType" to "LOVER",
                )

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc
                        .perform(
                            post("/api/v1/saju/partners")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(requestBody)),
                        ).andExpect(status().isUnauthorized)
                }
            }

            context("인증된 회원이 유효한 값으로 요청하면") {
                it("201로 생성하고 인증 주체의 memberId로 등록한다") {
                    val commandSlot = slot<RegisterPartnerSajuCommand>()
                    every { registerPartnerSajuUseCase.register(capture(commandSlot)) } returns PARTNER_SAJU_ID

                    mockMvc
                        .perform(
                            post("/api/v1/saju/partners")
                                .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList())))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(requestBody)),
                        ).andExpect(status().isCreated)

                    commandSlot.captured.memberId shouldBe MEMBER_ID
                    commandSlot.captured.relationshipType shouldBe "LOVER"
                }
            }

            context("인증됐지만 관계 값이 올바르지 않으면") {
                it("400을 반환한다") {
                    mockMvc
                        .perform(
                            post("/api/v1/saju/partners")
                                .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList())))
                                .contentType("application/json")
                                .content(
                                    objectMapper.writeValueAsString(requestBody + ("relationshipType" to "INVALID")),
                                ),
                        ).andExpect(status().isBadRequest)
                }
            }
        }
    }
}
