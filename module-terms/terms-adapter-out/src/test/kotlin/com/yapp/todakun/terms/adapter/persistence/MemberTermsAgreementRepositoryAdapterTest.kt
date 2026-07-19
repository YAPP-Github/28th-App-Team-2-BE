package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.terms.config.TestContainersConfig
import com.yapp.todakun.terms.fixture.TermsFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class MemberTermsAgreementRepositoryAdapterTest(
    private val memberTermsAgreementJpaRepository: MemberTermsAgreementJpaRepository,
) : DescribeSpec(
        {
            val adapter = MemberTermsAgreementRepositoryAdapter(memberTermsAgreementJpaRepository)

            beforeTest { memberTermsAgreementJpaRepository.deleteAll() }

            describe("saveAll") {
                context("여러 동의 내역을 저장하면") {
                    it("저장된 동의 내역을 반환한다") {
                        val agreements =
                            listOf(
                                TermsFixture.agreement(
                                    id = TermsFixture.SERVICE_ID,
                                    termsId = TermsFixture.SERVICE_ID,
                                    agreed = true,
                                ),
                                TermsFixture.agreement(
                                    id = TermsFixture.MARKETING_ID,
                                    termsId = TermsFixture.MARKETING_ID,
                                    agreed = false,
                                ),
                            )

                        val saved = adapter.saveAll(agreements)

                        saved shouldHaveSize 2
                        adapter.findAllByMemberId(TermsFixture.MEMBER_ID) shouldHaveSize 2
                    }
                }

                context("같은 id로 동의 결정을 갱신하면") {
                    it("행을 추가하지 않고 기존 내역을 덮어쓴다") {
                        val original = TermsFixture.agreement(termsId = TermsFixture.MARKETING_ID, agreed = false)
                        adapter.saveAll(listOf(original))

                        adapter.saveAll(listOf(original.updateDecision(agreed = true)))

                        val found = adapter.findAllByMemberId(TermsFixture.MEMBER_ID)
                        found shouldHaveSize 1
                        found.first().agreed shouldBe true
                    }
                }
            }

            describe("findAllByMemberId") {
                context("다른 회원의 동의 내역만 있으면") {
                    it("빈 목록을 반환한다") {
                        val otherMemberId = TermsFixture.PRIVACY_ID
                        adapter.saveAll(listOf(TermsFixture.agreement(memberId = otherMemberId)))

                        adapter.findAllByMemberId(TermsFixture.MEMBER_ID) shouldBe emptyList()
                    }
                }
            }
        },
    )
