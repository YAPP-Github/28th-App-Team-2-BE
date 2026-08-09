package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.EvictYearSelectionFortunesPort
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class ReplaceSelfSajuChartServiceTest : DescribeSpec({
    val manseryeokPort = mockk<ManseryeokPort>()
    val sajuChartRepository = mockk<SajuChartRepository>()
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val evictYearSelectionFortunesPort = mockk<EvictYearSelectionFortunesPort>()
    val service =
        ReplaceSelfSajuChartService(manseryeokPort, sajuChartRepository, memberSajuLinkRepository, evictYearSelectionFortunesPort)

    afterTest { clearMocks(manseryeokPort, sajuChartRepository, memberSajuLinkRepository, evictYearSelectionFortunesPort) }

    fun callReplace() =
        service.replace(
            memberId = SajuFixture.MEMBER_ID,
            name = "토닥이",
            gender = "FEMALE",
            calendarType = "SOLAR",
            birthDate = LocalDate.of(2001, 5, 30),
            birthTime = "MISI",
            isLeapMonth = false,
        )

    describe("replace") {
        context("기존 SELF 링크가 있으면") {
            it("새 명식을 저장하고 기존 명식을 삭제한 뒤 링크를 새 명식으로 갱신한다") {
                every { manseryeokPort.calculate(any(), any(), any(), any()) } returns SajuFixture.fourPillars()
                every { sajuChartRepository.save(any()) } answers { firstArg() }
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns SajuFixture.selfLink()
                every { sajuChartRepository.deleteById(SajuFixture.CHART_ID) } just Runs
                every { memberSajuLinkRepository.save(any()) } answers { firstArg() }
                every { evictYearSelectionFortunesPort.evictByMemberId(SajuFixture.MEMBER_ID) } just Runs

                callReplace()

                verify(exactly = 1) { sajuChartRepository.deleteById(SajuFixture.CHART_ID) }
                verify(exactly = 1) { memberSajuLinkRepository.save(match { it.role == SajuRole.SELF }) }
                verify(exactly = 1) { evictYearSelectionFortunesPort.evictByMemberId(SajuFixture.MEMBER_ID) }
            }
        }

        context("기존 SELF 링크가 없으면") {
            it("새로운 SELF 링크를 생성하고 삭제는 하지 않는다") {
                every { manseryeokPort.calculate(any(), any(), any(), any()) } returns SajuFixture.fourPillars()
                every { sajuChartRepository.save(any()) } answers { firstArg() }
                every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns null
                every { memberSajuLinkRepository.save(any()) } answers { firstArg() }
                every { evictYearSelectionFortunesPort.evictByMemberId(SajuFixture.MEMBER_ID) } just Runs

                callReplace()

                verify(exactly = 0) { sajuChartRepository.deleteById(any()) }
                verify(exactly = 1) { memberSajuLinkRepository.save(match { it.role == SajuRole.SELF }) }
                verify(exactly = 1) { evictYearSelectionFortunesPort.evictByMemberId(SajuFixture.MEMBER_ID) }
            }
        }
    }
})
