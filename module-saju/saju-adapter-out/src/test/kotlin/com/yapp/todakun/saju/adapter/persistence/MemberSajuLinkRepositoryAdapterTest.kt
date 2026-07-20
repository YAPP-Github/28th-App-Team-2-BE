package com.yapp.todakun.saju.adapter.persistence

import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class MemberSajuLinkRepositoryAdapterTest(
    private val memberSajuChartJpaRepository: MemberSajuChartJpaRepository,
) : DescribeSpec(
        {
            val adapter = MemberSajuLinkRepositoryAdapter(memberSajuChartJpaRepository)

            val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")
            val otherMemberId = UUID.fromString("018f0000-0000-7000-8000-000000000002")
            val chartA = UUID.fromString("018f0000-0000-7000-8000-0000000000a1")
            val chartB = UUID.fromString("018f0000-0000-7000-8000-0000000000b1")
            val chartC = UUID.fromString("018f0000-0000-7000-8000-0000000000c1")

            describe("findSelfByMemberId") {
                context("SELF 링크가 저장돼 있으면") {
                    it("해당 링크를 반환한다") {
                        val self = adapter.save(MemberSajuLink.self(memberId, chartA))

                        val found = adapter.findSelfByMemberId(memberId)

                        found.shouldNotBeNull()
                        found.id shouldBe self.id
                        found.role shouldBe SajuRole.SELF
                    }
                }
            }

            describe("findPartnersByMemberId / countPartnersByMemberId") {
                context("SELF와 PARTNER가 섞여 있으면") {
                    it("PARTNER만 반환한다") {
                        adapter.save(MemberSajuLink.self(memberId, chartA))
                        adapter.save(MemberSajuLink.partner(memberId, chartB, RelationshipType.LOVER))
                        adapter.save(MemberSajuLink.partner(memberId, chartC, RelationshipType.FRIEND))

                        adapter.findPartnersByMemberId(memberId).size shouldBe 2
                        adapter.countPartnersByMemberId(memberId) shouldBe 2L
                    }
                }
            }

            describe("findByIdAndMemberId") {
                context("다른 회원의 ID로 조회하면") {
                    it("null을 반환한다(소유권 격리)") {
                        val link = adapter.save(MemberSajuLink.partner(memberId, chartB, RelationshipType.LOVER))

                        adapter.findByIdAndMemberId(link.id, otherMemberId).shouldBeNull()
                        adapter.findByIdAndMemberId(link.id, memberId).shouldNotBeNull()
                    }
                }
            }

            describe("deleteByMemberId") {
                context("회원의 링크가 여러 개면") {
                    it("모두 삭제한다") {
                        adapter.save(MemberSajuLink.self(memberId, chartA))
                        adapter.save(MemberSajuLink.partner(memberId, chartB, RelationshipType.LOVER))

                        adapter.deleteByMemberId(memberId)

                        adapter.findSelfByMemberId(memberId).shouldBeNull()
                        adapter.countPartnersByMemberId(memberId) shouldBe 0L
                    }
                }
            }
        },
    )
