package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.PillarType
import com.yapp.todakun.saju.SajuPillar
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.PillarSummary
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class GetSajuChartServiceTest : DescribeSpec({
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val sajuChartRepository = mockk<SajuChartRepository>()
    val service = GetSajuChartService(memberSajuLinkRepository, sajuChartRepository)

    afterTest { clearMocks(memberSajuLinkRepository, sajuChartRepository) }

    describe("getChart") {
        context("SELF 링크와 명식이 있으면") {
            it("명식의 4주와 오행, 십성 분포를 SajuChartSummary로 변환해 반환한다") {
                val chart = SajuFixture.chart()
                val link = SajuFixture.selfLink(chartId = chart.id)
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns link
                every { sajuChartRepository.findById(chart.id) } returns chart

                val result = service.getChart(SajuFixture.MEMBER_ID)

                result.dayMaster shouldBe chart.dayMaster.name
                result.yearPillar shouldBe chart.pillars.first { it.pillarType == PillarType.YEAR }.toExpectedSummary()
                result.monthPillar shouldBe chart.pillars.first { it.pillarType == PillarType.MONTH }.toExpectedSummary()
                result.dayPillar shouldBe chart.pillars.first { it.pillarType == PillarType.DAY }.toExpectedSummary()
                result.hourPillar shouldBe chart.pillars.first { it.pillarType == PillarType.HOUR }.toExpectedSummary()
                result.dayPillar.stemSipseong shouldBe null // 일주 천간은 일원이라 십성 없음
                result.ohaeng shouldBe chart.ohaeng.associate { it.element.name to it.count }
                result.sipseong shouldBe chart.sipseong.associate { it.sipseong.name to it.count }
            }
        }

        context("시주(출생 시간)가 없는 명식이면") {
            it("hourPillar를 null로 반환한다") {
                val chart = SajuFixture.chart(birthTime = BirthTime.UNKNOWN)
                val link = SajuFixture.selfLink(chartId = chart.id)
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns link
                every { sajuChartRepository.findById(chart.id) } returns chart

                val result = service.getChart(SajuFixture.MEMBER_ID)

                result.hourPillar shouldBe null
            }
        }

        context("SELF 링크가 없으면") {
            it("SajuChartNotFoundException을 던진다") {
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns null

                shouldThrow<SajuChartNotFoundException> { service.getChart(SajuFixture.MEMBER_ID) }
            }
        }

        context("링크는 있지만 명식이 없으면") {
            it("SajuChartNotFoundException을 던진다") {
                val link = SajuFixture.selfLink()
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns link
                every { sajuChartRepository.findById(link.chartId) } returns null

                shouldThrow<SajuChartNotFoundException> { service.getChart(SajuFixture.MEMBER_ID) }
            }
        }
    }
})

/** [GetSajuChartService]의 private 매핑 로직을 재사용하지 않고 별도로 계산한다. */
private fun SajuPillar.toExpectedSummary(): PillarSummary =
    PillarSummary(
        stem = stem.name,
        branch = branch.name,
        stemSipseong = stemSipseong?.name,
        branchSipseong = branchSipseong.name,
        sibiunseong = sibiunseong.name,
    )
