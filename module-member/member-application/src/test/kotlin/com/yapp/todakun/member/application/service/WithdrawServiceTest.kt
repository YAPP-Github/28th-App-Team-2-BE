package com.yapp.todakun.member.application.service

import com.yapp.todakun.member.WithdrawalReason
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.port.inbound.WithdrawCommand
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.member.repository.MemberWithdrawalLogRepository
import com.yapp.todakun.shared.DeleteMemberSajuDataPort
import com.yapp.todakun.shared.RevokeMemberTokensPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class WithdrawServiceTest : DescribeSpec({
    val memberRepository = mockk<MemberRepository>()
    val memberWithdrawalLogRepository = mockk<MemberWithdrawalLogRepository>()
    val deleteMemberSajuDataPort = mockk<DeleteMemberSajuDataPort>()
    val revokeMemberTokensPort = mockk<RevokeMemberTokensPort>()
    val service =
        WithdrawService(memberRepository, memberWithdrawalLogRepository, deleteMemberSajuDataPort, revokeMemberTokensPort)

    afterTest {
        clearMocks(memberRepository, memberWithdrawalLogRepository, deleteMemberSajuDataPort, revokeMemberTokensPort)
    }

    val command = WithdrawCommand(MemberFixture.MEMBER_ID, WithdrawalReason.NOT_USING, detail = null)

    describe("withdraw") {
        context("회원이 존재하면") {
            it("사유 로그 저장 → 사주 삭제 → 토큰 폐기 → 회원 삭제 순서로 처리한다") {
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns MemberFixture.member()
                every { memberWithdrawalLogRepository.save(any()) } answers { firstArg() }
                every { deleteMemberSajuDataPort.deleteByMemberId(MemberFixture.MEMBER_ID) } just Runs
                every { revokeMemberTokensPort.revokeAll(MemberFixture.MEMBER_ID) } just Runs
                every { memberRepository.deleteById(MemberFixture.MEMBER_ID) } just Runs

                service.withdraw(command)

                verifyOrder {
                    memberWithdrawalLogRepository.save(any())
                    deleteMemberSajuDataPort.deleteByMemberId(MemberFixture.MEMBER_ID)
                    revokeMemberTokensPort.revokeAll(MemberFixture.MEMBER_ID)
                    memberRepository.deleteById(MemberFixture.MEMBER_ID)
                }
            }
        }

        context("회원이 존재하지 않으면") {
            it("MemberNotFoundException을 던지고 아무것도 삭제하지 않는다") {
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns null

                shouldThrow<MemberNotFoundException> { service.withdraw(command) }

                verify(exactly = 0) { memberRepository.deleteById(any()) }
                verify(exactly = 0) { deleteMemberSajuDataPort.deleteByMemberId(any()) }
            }
        }
    }
})
