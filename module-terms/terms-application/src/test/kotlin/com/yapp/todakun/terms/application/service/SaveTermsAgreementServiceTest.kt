package com.yapp.todakun.terms.application.service

import com.yapp.todakun.terms.MemberTermsAgreement
import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.exception.DuplicateTermsAgreementException
import com.yapp.todakun.terms.exception.RequiredTermsNotAgreedException
import com.yapp.todakun.terms.exception.TermsNotFoundException
import com.yapp.todakun.terms.port.inbound.SaveTermsAgreementCommand
import com.yapp.todakun.terms.port.inbound.TermsAgreementItem
import com.yapp.todakun.terms.repository.MemberTermsAgreementRepository
import com.yapp.todakun.terms.repository.TermsRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID

class SaveTermsAgreementServiceTest :
    DescribeSpec(
        {
            val termsRepository = mockk<TermsRepository>()
            val memberTermsAgreementRepository = mockk<MemberTermsAgreementRepository>()
            val service = SaveTermsAgreementService(termsRepository, memberTermsAgreementRepository)

            afterTest { clearMocks(termsRepository, memberTermsAgreementRepository) }

            val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")
            val serviceId = UUID.fromString("018f0000-0000-7000-8000-0000000000a1")
            val privacyId = UUID.fromString("018f0000-0000-7000-8000-0000000000a2")
            val marketingId = UUID.fromString("018f0000-0000-7000-8000-0000000000a3")
            val unknownId = UUID.fromString("018f0000-0000-7000-8000-0000000000ff")

            val catalog =
                listOf(
                    Terms.reconstitute(serviceId, TermsType.SERVICE, "서비스 이용약관", required = true),
                    Terms.reconstitute(privacyId, TermsType.PRIVACY, "개인정보 수집·이용 동의", required = true),
                    Terms.reconstitute(marketingId, TermsType.MARKETING, "마케팅 정보 수신 동의", required = false),
                )

            describe("save") {
                context("필수 약관에 모두 동의한 신규 제출이면") {
                    it("모든 약관을 새로 생성해 저장하고 저장 결과를 반환한다") {
                        every { termsRepository.findAll() } returns catalog
                        every { memberTermsAgreementRepository.findAllByMemberId(memberId) } returns emptyList()
                        val savedSlot = slot<List<MemberTermsAgreement>>()
                        every { memberTermsAgreementRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

                        val command =
                            SaveTermsAgreementCommand(
                                memberId = memberId,
                                items =
                                    listOf(
                                        TermsAgreementItem(serviceId, agreed = true),
                                        TermsAgreementItem(privacyId, agreed = true),
                                        TermsAgreementItem(marketingId, agreed = false),
                                    ),
                            )

                        val result = service.save(command)

                        result.size shouldBe 3
                        savedSlot.captured.map { it.termsId } shouldContainExactlyInAnyOrder
                            listOf(serviceId, privacyId, marketingId)
                        savedSlot.captured.first { it.termsId == marketingId }.agreed shouldBe false
                        savedSlot.captured.all { it.memberId == memberId } shouldBe true
                    }
                }

                context("이미 제출한 약관을 다시 제출하면") {
                    it("기존 동의 내역의 id를 유지한 채 결정만 갱신한다") {
                        val existing =
                            MemberTermsAgreement.reconstitute(
                                id = UUID.fromString("018f0000-0000-7000-8000-0000000000b1"),
                                memberId = memberId,
                                termsId = marketingId,
                                agreed = false,
                            )
                        every { termsRepository.findAll() } returns catalog
                        every { memberTermsAgreementRepository.findAllByMemberId(memberId) } returns listOf(existing)
                        val savedSlot = slot<List<MemberTermsAgreement>>()
                        every { memberTermsAgreementRepository.saveAll(capture(savedSlot)) } answers { savedSlot.captured }

                        val command =
                            SaveTermsAgreementCommand(
                                memberId = memberId,
                                items =
                                    listOf(
                                        TermsAgreementItem(serviceId, agreed = true),
                                        TermsAgreementItem(privacyId, agreed = true),
                                        TermsAgreementItem(marketingId, agreed = true),
                                    ),
                            )

                        service.save(command)

                        val updatedMarketing = savedSlot.captured.first { it.termsId == marketingId }
                        updatedMarketing.id shouldBe existing.id
                        updatedMarketing.agreed shouldBe true
                    }
                }

                context("필수 약관에 동의하지 않으면") {
                    it("RequiredTermsNotAgreedException을 던지고 저장하지 않는다") {
                        every { termsRepository.findAll() } returns catalog

                        val command =
                            SaveTermsAgreementCommand(
                                memberId = memberId,
                                items =
                                    listOf(
                                        TermsAgreementItem(serviceId, agreed = true),
                                        TermsAgreementItem(privacyId, agreed = false),
                                    ),
                            )

                        shouldThrow<RequiredTermsNotAgreedException> { service.save(command) }

                        verify(exactly = 0) { memberTermsAgreementRepository.saveAll(any()) }
                    }
                }

                context("존재하지 않는 약관 ID가 포함되면") {
                    it("TermsNotFoundException을 던지고 저장하지 않는다") {
                        every { termsRepository.findAll() } returns catalog

                        val command =
                            SaveTermsAgreementCommand(
                                memberId = memberId,
                                items =
                                    listOf(
                                        TermsAgreementItem(serviceId, agreed = true),
                                        TermsAgreementItem(privacyId, agreed = true),
                                        TermsAgreementItem(unknownId, agreed = true),
                                    ),
                            )

                        shouldThrow<TermsNotFoundException> { service.save(command) }

                        verify(exactly = 0) { memberTermsAgreementRepository.saveAll(any()) }
                    }
                }

                context("같은 약관 ID가 중복 제출되면") {
                    it("DuplicateTermsAgreementException을 던지고 저장하지 않는다") {
                        every { termsRepository.findAll() } returns catalog

                        val command =
                            SaveTermsAgreementCommand(
                                memberId = memberId,
                                items =
                                    listOf(
                                        TermsAgreementItem(serviceId, agreed = true),
                                        TermsAgreementItem(privacyId, agreed = true),
                                        TermsAgreementItem(serviceId, agreed = false),
                                    ),
                            )

                        shouldThrow<DuplicateTermsAgreementException> { service.save(command) }

                        verify(exactly = 0) { memberTermsAgreementRepository.saveAll(any()) }
                    }
                }
            }
        },
    )
