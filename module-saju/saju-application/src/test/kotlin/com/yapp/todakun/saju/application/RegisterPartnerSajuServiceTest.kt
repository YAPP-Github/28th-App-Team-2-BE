package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.exception.PartnerSajuLimitExceededException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.inbound.RegisterPartnerSajuCommand
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class RegisterPartnerSajuServiceTest : DescribeSpec({
    val manseryeokPort = mockk<ManseryeokPort>()
    val sajuChartRepository = mockk<SajuChartRepository>()
    val memberSajuLinkRepository = mockk<MemberSajuLinkRepository>()
    val service = RegisterPartnerSajuService(manseryeokPort, sajuChartRepository, memberSajuLinkRepository)

    afterTest { clearMocks(manseryeokPort, sajuChartRepository, memberSajuLinkRepository) }

    val command =
        RegisterPartnerSajuCommand(
            memberId = SajuFixture.MEMBER_ID,
            name = "토실이",
            gender = "MALE",
            calendarType = "SOLAR",
            birthDate = LocalDate.of(1999, 2, 13),
            birthTime = "SINSI",
            relationshipType = "LOVER",
        )

    describe("register") {
        context("등록된 상대가 10명 미만이면") {
            it("명식을 계산·저장하고 PARTNER 링크를 생성한다") {
                every { memberSajuLinkRepository.countPartnersByMemberId(SajuFixture.MEMBER_ID) } returns 3L
                every { manseryeokPort.calculate(any(), any(), any(), any()) } returns SajuFixture.fourPillars()
                every { sajuChartRepository.save(any()) } answers { firstArg() }
                every { memberSajuLinkRepository.save(any()) } answers { firstArg() }

                service.register(command)

                verify(exactly = 1) { sajuChartRepository.save(any()) }
                verify(exactly = 1) {
                    memberSajuLinkRepository.save(
                        match { it.role == SajuRole.PARTNER && it.relationshipType == RelationshipType.LOVER },
                    )
                }
            }
        }

        context("등록된 상대가 이미 10명이면") {
            it("PartnerSajuLimitExceededException을 던지고 저장하지 않는다") {
                every { memberSajuLinkRepository.countPartnersByMemberId(SajuFixture.MEMBER_ID) } returns 10L

                shouldThrow<PartnerSajuLimitExceededException> { service.register(command) }

                verify(exactly = 0) { sajuChartRepository.save(any()) }
                verify(exactly = 0) { memberSajuLinkRepository.save(any()) }
            }
        }
    }
})
