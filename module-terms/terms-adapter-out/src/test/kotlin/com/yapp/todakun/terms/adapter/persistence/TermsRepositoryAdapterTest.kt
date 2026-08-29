package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.config.TestContainersConfig
import com.yapp.todakun.terms.fixture.TermsFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class TermsRepositoryAdapterTest(
    private val termsJpaRepository: TermsJpaRepository,
) : DescribeSpec(
        {
            val adapter = TermsRepositoryAdapter(termsJpaRepository)

            describe("findAll") {
                context("등록된 약관이 있으면") {
                    it("모든 약관을 도메인으로 변환해 반환한다") {
                        termsJpaRepository.deleteAll()
                        val service = TermsFixture.terms(id = TermsFixture.SERVICE_ID, type = TermsType.SERVICE, required = true)
                        val marketing =
                            TermsFixture.terms(
                                id = TermsFixture.MARKETING_ID,
                                type = TermsType.MARKETING,
                                title = "마케팅 정보 수신 동의",
                                required = false,
                            )
                        termsJpaRepository.save(TermsJpaEntity.fromDomain(service))
                        termsJpaRepository.save(TermsJpaEntity.fromDomain(marketing))

                        val found = adapter.findAll()

                        found shouldContainExactlyInAnyOrder listOf(service, marketing)
                    }
                }

                context("등록된 약관이 없으면") {
                    it("빈 목록을 반환한다") {
                        termsJpaRepository.deleteAll()

                        adapter.findAll() shouldBe emptyList()
                    }
                }
            }
        },
    )
