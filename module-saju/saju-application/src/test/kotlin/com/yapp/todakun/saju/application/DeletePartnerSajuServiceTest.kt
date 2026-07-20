package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class DeletePartnerSajuServiceTest : DescribeSpec({
    val sajuChartRepository = mockk<SajuChartRepository>()
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val service = DeletePartnerSajuService(sajuChartRepository, memberSajuLinkRepository)

    afterTest { clearMocks(sajuChartRepository, memberSajuLinkRepository) }

    describe("delete") {
        context("본인이 소유한 PARTNER 링크면") {
            it("명식과 링크를 함께 삭제한다") {
                every {
                    memberSajuLinkRepository.findByIdAndMemberId(SajuFixture.LINK_ID, SajuFixture.MEMBER_ID)
                } returns SajuFixture.partnerLink()
                every { sajuChartRepository.deleteById(SajuFixture.CHART_ID) } just Runs
                every { memberSajuLinkRepository.deleteById(SajuFixture.LINK_ID) } just Runs

                service.delete(SajuFixture.MEMBER_ID, SajuFixture.LINK_ID)

                verify(exactly = 1) { sajuChartRepository.deleteById(SajuFixture.CHART_ID) }
                verify(exactly = 1) { memberSajuLinkRepository.deleteById(SajuFixture.LINK_ID) }
            }
        }

        context("본인 소유의 PARTNER 링크가 아니면") {
            it("SajuChartNotFoundException을 던지고 삭제하지 않는다") {
                every {
                    memberSajuLinkRepository.findByIdAndMemberId(SajuFixture.LINK_ID, SajuFixture.MEMBER_ID)
                } returns SajuFixture.selfLink()

                shouldThrow<SajuChartNotFoundException> { service.delete(SajuFixture.MEMBER_ID, SajuFixture.LINK_ID) }

                verify(exactly = 0) { sajuChartRepository.deleteById(any()) }
            }
        }
    }
})
