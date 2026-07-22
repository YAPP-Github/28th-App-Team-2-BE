package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class GetMySajuServiceTest : DescribeSpec({
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val sajuChartRepository = mockk<SajuChartRepository>()
    val service = GetMySajuService(memberSajuLinkRepository, sajuChartRepository)

    afterTest { clearMocks(memberSajuLinkRepository, sajuChartRepository) }

    describe("getMine") {
        context("SELF 링크와 명식이 있으면") {
            it("본인 만세력 상세(4주·파생 포함)를 반환한다") {
                val chart = SajuFixture.chart()
                val link = SajuFixture.selfLink(chartId = chart.id)
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns link
                every { sajuChartRepository.findById(chart.id) } returns chart

                val result = service.getMine(SajuFixture.MEMBER_ID)

                result.linkId shouldBe link.id
                result.role shouldBe SajuRole.SELF
                result.pillars.size shouldBe 4
            }
        }

        context("SELF 링크가 없으면") {
            it("SajuChartNotFoundException을 던진다") {
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns null

                shouldThrow<SajuChartNotFoundException> { service.getMine(SajuFixture.MEMBER_ID) }
            }
        }
    }
})
