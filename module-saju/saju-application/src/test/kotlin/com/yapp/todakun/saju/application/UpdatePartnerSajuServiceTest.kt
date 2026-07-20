package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.inbound.UpdatePartnerSajuCommand
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
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
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class UpdatePartnerSajuServiceTest : DescribeSpec({
    val manseryeokPort = mockk<ManseryeokPort>()
    val sajuChartRepository = mockk<SajuChartRepository>()
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val service = UpdatePartnerSajuService(manseryeokPort, sajuChartRepository, memberSajuLinkRepository)

    afterTest { clearMocks(manseryeokPort, sajuChartRepository, memberSajuLinkRepository) }

    val command =
        UpdatePartnerSajuCommand(
            memberId = SajuFixture.MEMBER_ID,
            linkId = SajuFixture.LINK_ID,
            name = "토실이",
            gender = "MALE",
            calendarType = "SOLAR",
            birthDate = LocalDate.of(1999, 2, 13),
            birthTime = "SINSI",
            relationshipType = "FRIEND",
        )

    describe("update") {
        context("본인이 소유한 PARTNER 링크면") {
            it("명식을 재계산해 교체하고 관계 라벨을 갱신한다") {
                every {
                    memberSajuLinkRepository.findByIdAndMemberId(SajuFixture.LINK_ID, SajuFixture.MEMBER_ID)
                } returns SajuFixture.partnerLink()
                every { manseryeokPort.calculate(any(), any(), any(), any()) } returns SajuFixture.fourPillars()
                every { sajuChartRepository.save(any()) } answers { firstArg() }
                every { sajuChartRepository.deleteById(SajuFixture.CHART_ID) } just Runs
                every { memberSajuLinkRepository.save(any()) } answers { firstArg() }

                service.update(command)

                verify(exactly = 1) { sajuChartRepository.deleteById(SajuFixture.CHART_ID) }
                verify(exactly = 1) {
                    memberSajuLinkRepository.save(match { it.relationshipType == RelationshipType.FRIEND })
                }
            }
        }

        context("링크가 없거나 본인 소유가 아니면") {
            it("SajuChartNotFoundException을 던진다") {
                every {
                    memberSajuLinkRepository.findByIdAndMemberId(SajuFixture.LINK_ID, SajuFixture.MEMBER_ID)
                } returns null

                shouldThrow<SajuChartNotFoundException> { service.update(command) }

                verify(exactly = 0) { sajuChartRepository.save(any()) }
            }
        }

        context("대상이 SELF 링크면") {
            it("SajuChartNotFoundException을 던진다(상대 전용 API)") {
                every {
                    memberSajuLinkRepository.findByIdAndMemberId(SajuFixture.LINK_ID, SajuFixture.MEMBER_ID)
                } returns SajuFixture.selfLink()

                shouldThrow<SajuChartNotFoundException> { service.update(command) }
            }
        }
    }
})
