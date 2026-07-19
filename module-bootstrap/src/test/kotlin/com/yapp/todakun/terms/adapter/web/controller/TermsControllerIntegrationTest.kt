package com.yapp.todakun.terms.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.terms.MemberTermsAgreement
import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.port.inbound.GetTermsUseCase
import com.yapp.todakun.terms.port.inbound.SaveTermsAgreementCommand
import com.yapp.todakun.terms.port.inbound.SaveTermsAgreementUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
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
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val SERVICE_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000a1")
private val PRIVACY_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000a2")
private val AGREEMENT_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000b1")

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestContainersConfig::class)
class TermsControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : DescribeSpec() {
    @MockkBean
    private lateinit var getTermsUseCase: GetTermsUseCase

    @MockkBean
    private lateinit var saveTermsAgreementUseCase: SaveTermsAgreementUseCase

    init {
        afterTest { clearMocks(getTermsUseCase, saveTermsAgreementUseCase) }

        describe("GET /api/v1/terms") {
            context("인증 없이 요청해도(공개 API)") {
                it("약관 목록을 조회한다") {
                    every { getTermsUseCase.getAllTerms() } returns
                        listOf(Terms.reconstitute(SERVICE_ID, TermsType.SERVICE, "서비스 이용약관", required = true))

                    val response =
                        mockMvc
                            .perform(get("/api/v1/terms"))
                            .andExpect(status().isOk)
                            .andReturn()
                            .response.contentAsString

                    val body = objectMapper.readTree(response)
                    body["success"].asBoolean() shouldBe true
                    body["data"][0]["type"].asString() shouldBe "SERVICE"
                    body["data"][0]["required"].asBoolean() shouldBe true
                }
            }
        }

        describe("POST /api/v1/terms/agreements") {
            val requestBody =
                mapOf(
                    "agreements" to
                        listOf(
                            mapOf("termsId" to SERVICE_ID, "agreed" to true),
                            mapOf("termsId" to PRIVACY_ID, "agreed" to true),
                        ),
                )

            context("인증 없이 요청하면") {
                it("401을 반환한다") {
                    mockMvc
                        .perform(
                            post("/api/v1/terms/agreements")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(requestBody)),
                        ).andExpect(status().isUnauthorized)

                    verify(exactly = 0) { saveTermsAgreementUseCase.save(any()) }
                }
            }

            context("인증된 회원이 필수 약관에 모두 동의하면") {
                it("201로 저장 결과를 반환하고 인증 주체의 memberId로 저장한다") {
                    val commandSlot = slot<SaveTermsAgreementCommand>()
                    every { saveTermsAgreementUseCase.save(capture(commandSlot)) } answers
                        {
                            commandSlot.captured.items.map {
                                MemberTermsAgreement.reconstitute(
                                    id = AGREEMENT_ID,
                                    memberId = commandSlot.captured.memberId,
                                    termsId = it.termsId,
                                    agreed = it.agreed,
                                )
                            }
                        }

                    val response =
                        mockMvc
                            .perform(
                                post("/api/v1/terms/agreements")
                                    .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList())))
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(requestBody)),
                            ).andExpect(status().isCreated)
                            .andReturn()
                            .response.contentAsString

                    val body = objectMapper.readTree(response)
                    body["success"].asBoolean() shouldBe true
                    body["data"][0]["termsId"].asString() shouldBe SERVICE_ID.toString()
                    commandSlot.captured.memberId shouldBe MEMBER_ID
                }
            }

            context("인증됐지만 동의 내역이 비어 있으면") {
                it("400을 반환한다") {
                    mockMvc
                        .perform(
                            post("/api/v1/terms/agreements")
                                .with(authentication(UsernamePasswordAuthenticationToken(MEMBER_ID, null, emptyList())))
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(mapOf("agreements" to emptyList<Any>()))),
                        ).andExpect(status().isBadRequest)

                    verify(exactly = 0) { saveTermsAgreementUseCase.save(any()) }
                }
            }
        }
    }
}
