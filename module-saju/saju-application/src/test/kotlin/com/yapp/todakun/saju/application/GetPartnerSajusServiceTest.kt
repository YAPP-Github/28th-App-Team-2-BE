package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.inbound.PartnerSajuSummary
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

private val PARTNER_CHART_ID_1: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000c1")
private val PARTNER_CHART_ID_2: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000c2")
private val PARTNER_LINK_ID_1: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000d1")
private val PARTNER_LINK_ID_2: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000d2")

class GetPartnerSajusServiceTest : DescribeSpec({
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val sajuChartRepository = mockk<SajuChartRepository>()
    val service = GetPartnerSajusService(memberSajuLinkRepository, sajuChartRepository)

    afterTest { clearMocks(memberSajuLinkRepository, sajuChartRepository) }

    describe("getPartners") {
        context("등록된 상대방이 여러 명이면") {
            it("링크 순서를 유지한 채 각 링크에 대응하는 요약 정보를 배치 조회 한 번으로 반환한다") {
                val link1 = SajuFixture.partnerLink(id = PARTNER_LINK_ID_1, chartId = PARTNER_CHART_ID_1)
                val link2 = SajuFixture.partnerLink(id = PARTNER_LINK_ID_2, chartId = PARTNER_CHART_ID_2)
                val summary1 = SajuFixture.chartSummary(id = PARTNER_CHART_ID_1, name = "상대방1")
                val summary2 = SajuFixture.chartSummary(id = PARTNER_CHART_ID_2, name = "상대방2")
                every { memberSajuLinkRepository.findPartnersByMemberId(SajuFixture.MEMBER_ID) } returns listOf(link1, link2)
                every {
                    sajuChartRepository.findSummariesByIds(listOf(PARTNER_CHART_ID_1, PARTNER_CHART_ID_2))
                } returns listOf(summary1, summary2)

                val result = service.getPartners(SajuFixture.MEMBER_ID)

                result shouldBe listOf(PartnerSajuSummary.from(link1, summary1), PartnerSajuSummary.from(link2, summary2))
                verify(exactly = 1) { sajuChartRepository.findSummariesByIds(listOf(PARTNER_CHART_ID_1, PARTNER_CHART_ID_2)) }
            }
        }

        context("링크는 있지만 대응하는 명식 요약이 없으면") {
            it("SajuChartNotFoundException을 던진다") {
                val link = SajuFixture.partnerLink()
                every { memberSajuLinkRepository.findPartnersByMemberId(SajuFixture.MEMBER_ID) } returns listOf(link)
                every { sajuChartRepository.findSummariesByIds(listOf(link.chartId)) } returns emptyList()

                shouldThrow<SajuChartNotFoundException> { service.getPartners(SajuFixture.MEMBER_ID) }
            }
        }

        context("등록된 상대방이 없으면") {
            it("빈 리스트를 반환한다") {
                every { memberSajuLinkRepository.findPartnersByMemberId(SajuFixture.MEMBER_ID) } returns emptyList()
                every { sajuChartRepository.findSummariesByIds(emptyList()) } returns emptyList()

                service.getPartners(SajuFixture.MEMBER_ID) shouldBe emptyList()
            }
        }
    }
})
