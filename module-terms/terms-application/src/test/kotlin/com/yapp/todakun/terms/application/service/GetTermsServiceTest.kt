package com.yapp.todakun.terms.application.service

import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.repository.TermsRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class GetTermsServiceTest :
    DescribeSpec(
        {
            val termsRepository = mockk<TermsRepository>()
            val getTermsService = GetTermsService(termsRepository)

            describe("getAllTerms") {
                context("등록된 약관이 있으면") {
                    it("전체 약관 목록을 반환한다") {
                        val terms =
                            listOf(
                                Terms.reconstitute(
                                    UUID.fromString("018f0000-0000-7000-8000-0000000000a1"),
                                    TermsType.SERVICE,
                                    "서비스 이용약관",
                                    required = true,
                                ),
                                Terms.reconstitute(
                                    UUID.fromString("018f0000-0000-7000-8000-0000000000a3"),
                                    TermsType.MARKETING,
                                    "마케팅 정보 수신 동의",
                                    required = false,
                                ),
                            )
                        every { termsRepository.findAll() } returns terms

                        getTermsService.getAllTerms() shouldBe terms
                    }
                }
            }
        },
    )
