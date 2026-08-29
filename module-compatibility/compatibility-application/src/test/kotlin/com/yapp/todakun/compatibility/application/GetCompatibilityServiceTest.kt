package com.yapp.todakun.compatibility.application

import com.yapp.todakun.compatibility.CompatibilityElement
import com.yapp.todakun.compatibility.CompatibilityOhaeng
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.exception.CompatibilityNotFoundException
import com.yapp.todakun.compatibility.port.outbound.SajuCompatibilityRepository
import com.yapp.todakun.shared.GetSajuChartNamePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class GetCompatibilityServiceTest :
    DescribeSpec(
        {
            val sajuCompatibilityRepository = mockk<SajuCompatibilityRepository>()
            val getSajuChartNamePort = mockk<GetSajuChartNamePort>()
            val getCompatibilityService = GetCompatibilityService(sajuCompatibilityRepository, getSajuChartNamePort)

            afterTest { clearMocks(sajuCompatibilityRepository, getSajuChartNamePort) }

            describe("getById") {
                context("존재하는 id이고 소유자가 일치하면") {
                    it("상대 명식에서 조회한 이름과 함께 궁합을 반환한다") {
                        val compatibility = sajuCompatibility()
                        every { sajuCompatibilityRepository.findById(compatibility.id) } returns compatibility
                        every { getSajuChartNamePort.getName(compatibility.partnerChartId) } returns "토실이"

                        val result = getCompatibilityService.getById(compatibility.id, compatibility.memberId)

                        result.id shouldBe compatibility.id
                        result.partnerName shouldBe "토실이"
                        result.relationshipType shouldBe compatibility.relationshipType
                        result.score shouldBe compatibility.score
                        result.headline shouldBe compatibility.headline
                        result.subheadline shouldBe compatibility.subheadline
                        result.summary shouldBe compatibility.summary
                        result.totalAnalysis shouldBe compatibility.totalAnalysis
                        result.analysisBasis shouldBe compatibility.analysisBasis
                        result.ohaengs shouldBe compatibility.ohaengs
                    }
                }

                context("존재하지 않는 id이면") {
                    it("CompatibilityNotFoundException을 던진다") {
                        val nonExistentId = UUID.fromString("018f0000-0000-7000-8000-0000000000ff")
                        val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")
                        every { sajuCompatibilityRepository.findById(nonExistentId) } returns null

                        shouldThrow<CompatibilityNotFoundException> {
                            getCompatibilityService.getById(nonExistentId, memberId)
                        }
                    }
                }

                context("다른 회원의 궁합이면") {
                    it("CompatibilityNotFoundException을 던진다") {
                        val compatibility = sajuCompatibility()
                        val otherMemberId = UUID.fromString("018f0000-0000-7000-8000-0000000000fe")
                        every { sajuCompatibilityRepository.findById(compatibility.id) } returns compatibility

                        shouldThrow<CompatibilityNotFoundException> {
                            getCompatibilityService.getById(compatibility.id, otherMemberId)
                        }
                    }
                }
            }
        },
    )

private fun sajuCompatibility(): SajuCompatibility =
    SajuCompatibility.reconstitute(
        id = UUID.fromString("018f0000-0000-7000-8000-0000000000e1"),
        memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001"),
        myChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c1"),
        partnerChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c2"),
        relationshipType = CompatibilityRelationshipType.LOVER,
        score = 85,
        headline = "함께할수록 빛나는 궁합",
        subheadline = "함께 있을 때, 편안함이 커지는 사이예요.",
        summary = "두 분은 서로의 부족한 기운을 보완합니다.",
        totalAnalysis = "총운 분석 내용",
        analysisBasis = "사주 팔자 기반",
        ohaengs = CompatibilityElement.entries.map { CompatibilityOhaeng(it, 20) },
    )
