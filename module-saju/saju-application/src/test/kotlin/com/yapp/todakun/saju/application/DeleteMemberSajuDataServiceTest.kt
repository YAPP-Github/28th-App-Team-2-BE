package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class DeleteMemberSajuDataServiceTest : DescribeSpec({
    val sajuChartRepository = mockk<SajuChartRepository>()
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val service = DeleteMemberSajuDataService(sajuChartRepository, memberSajuLinkRepository)

    afterTest { clearMocks(sajuChartRepository, memberSajuLinkRepository) }

    describe("deleteByMemberId") {
        it("본인·상대 명식을 모두 삭제하고 회원의 링크를 제거한다") {
            val partnerChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c2")
            val self = SajuFixture.selfLink()
            val partner =
                SajuFixture.partnerLink(
                    id = UUID.fromString("018f0000-0000-7000-8000-0000000000d2"),
                    chartId = partnerChartId,
                )
            every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns self
            every { memberSajuLinkRepository.findPartnersByMemberId(SajuFixture.MEMBER_ID) } returns listOf(partner)
            every { sajuChartRepository.deleteAllByIds(any()) } just Runs
            every { memberSajuLinkRepository.deleteByMemberId(SajuFixture.MEMBER_ID) } just Runs

            service.deleteByMemberId(SajuFixture.MEMBER_ID)

            verify(exactly = 1) {
                sajuChartRepository.deleteAllByIds(match { it.toSet() == setOf(SajuFixture.CHART_ID, partnerChartId) })
            }
            verify(exactly = 1) { memberSajuLinkRepository.deleteByMemberId(SajuFixture.MEMBER_ID) }
        }
    }
})
