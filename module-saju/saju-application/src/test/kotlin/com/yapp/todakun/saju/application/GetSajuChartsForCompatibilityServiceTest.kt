package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class GetSajuChartsForCompatibilityServiceTest : DescribeSpec({
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val sajuChartRepository = mockk<SajuChartRepository>()
    val service = GetSajuChartsForCompatibilityService(memberSajuLinkRepository, sajuChartRepository)

    val partnerLinkId = UUID.fromString("018f0000-0000-7000-8000-0000000000d2")

    afterTest { clearMocks(memberSajuLinkRepository, sajuChartRepository) }

    describe("getCharts") {
        context("SELF·PARTNER 링크와 두 명식이 모두 있으면") {
            it("두 명식 뷰(오행 코드 key)와 상대 이름·관계 라벨을 담아 반환한다") {
                val myChart = SajuFixture.chart(name = "토닥이")
                val partnerChart = SajuFixture.chart(name = "토실이")
                val selfLink = SajuFixture.selfLink(chartId = myChart.id)
                val partnerLink =
                    SajuFixture.partnerLink(id = partnerLinkId, chartId = partnerChart.id, relationshipType = RelationshipType.LOVER)
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns selfLink
                every { memberSajuLinkRepository.findByIdAndMemberId(partnerLinkId, SajuFixture.MEMBER_ID) } returns partnerLink
                every { sajuChartRepository.findById(myChart.id) } returns myChart
                every { sajuChartRepository.findById(partnerChart.id) } returns partnerChart

                val result = service.getCharts(SajuFixture.MEMBER_ID, partnerLinkId)

                result.myChartId shouldBe myChart.id
                result.partnerChartId shouldBe partnerChart.id
                result.partnerName shouldBe "토실이"
                result.relationshipType shouldBe "LOVER"
                result.myChart.ohaeng shouldContainKey "WATER"
                result.myChart.ohaeng.values.sum() shouldBe 8
            }
        }

        context("SELF 링크가 없으면") {
            it("SajuChartNotFoundException을 던진다") {
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns null

                shouldThrow<SajuChartNotFoundException> { service.getCharts(SajuFixture.MEMBER_ID, partnerLinkId) }
            }
        }

        context("상대 링크가 PARTNER가 아니면(본인 링크이면)") {
            it("SajuChartNotFoundException을 던진다") {
                val myChart = SajuFixture.chart(name = "토닥이")
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns
                    SajuFixture.selfLink(chartId = myChart.id)
                every {
                    memberSajuLinkRepository.findByIdAndMemberId(partnerLinkId, SajuFixture.MEMBER_ID)
                } returns SajuFixture.selfLink(id = partnerLinkId)

                shouldThrow<SajuChartNotFoundException> { service.getCharts(SajuFixture.MEMBER_ID, partnerLinkId) }
            }
        }
    }
})
