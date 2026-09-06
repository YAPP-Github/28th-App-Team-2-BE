package com.yapp.todakun.member.application.service

import com.yapp.todakun.member.WithdrawalReason
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.port.inbound.WithdrawMemberCommand
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.member.repository.MemberWithdrawalLogRepository
import com.yapp.todakun.shared.DeleteMemberSajusPort
import com.yapp.todakun.shared.OauthRevokeCredential
import com.yapp.todakun.shared.RegisterWithdrawnAccountPort
import com.yapp.todakun.shared.RevokeMemberTokensPort
import com.yapp.todakun.shared.RevokeOauthTokenPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class WithdrawMemberTransactionServiceTest : DescribeSpec({
    val memberRepository = mockk<MemberRepository>()
    val memberWithdrawalLogRepository = mockk<MemberWithdrawalLogRepository>()
    val deleteMemberSajusPort = mockk<DeleteMemberSajusPort>()
    val revokeMemberTokensPort = mockk<RevokeMemberTokensPort>()
    val registerWithdrawnAccountPort = mockk<RegisterWithdrawnAccountPort>()
    val revokeOauthTokenPort = mockk<RevokeOauthTokenPort>()
    val service =
        WithdrawMemberTransactionService(
            memberRepository,
            memberWithdrawalLogRepository,
            deleteMemberSajusPort,
            revokeMemberTokensPort,
            registerWithdrawnAccountPort,
            revokeOauthTokenPort,
        )

    afterTest {
        clearMocks(
            memberRepository,
            memberWithdrawalLogRepository,
            deleteMemberSajusPort,
            revokeMemberTokensPort,
            registerWithdrawnAccountPort,
            revokeOauthTokenPort,
        )
    }

    val jti = "test-jti"
    val remainingSeconds = 3600L
    val command =
        WithdrawMemberCommand(
            MemberFixture.MEMBER_ID,
            WithdrawalReason.LOW_USAGE,
            detail = null,
            jti = jti,
            remainingSeconds = remainingSeconds,
        )

    describe("withdraw") {
        context("회원이 존재하면") {
            it("사유 로그 저장 → 사주 삭제 → 토큰 폐기 → 재가입 제한 등록 → OAuth 자격증명 삭제 → 회원 삭제 순서로 처리하고 revoke에 필요한 자격증명을 반환한다") {
                val member = MemberFixture.member()
                val oauthRevokeCredential = OauthRevokeCredential(member.providerId, "client-id", "refresh-token")
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns member
                every { memberWithdrawalLogRepository.save(any()) } answers { firstArg() }
                every { deleteMemberSajusPort.deleteByMemberId(MemberFixture.MEMBER_ID) } just Runs
                every { revokeMemberTokensPort.revokeAll(MemberFixture.MEMBER_ID, jti, remainingSeconds) } just Runs
                every { registerWithdrawnAccountPort.register(member.oauthProvider, member.providerId) } just Runs
                every { revokeOauthTokenPort.prepareRevoke(member.oauthProvider, member.providerId) } returns oauthRevokeCredential
                every { memberRepository.deleteById(MemberFixture.MEMBER_ID) } just Runs

                val result = service.withdraw(command)

                result shouldBe oauthRevokeCredential
                verifyOrder {
                    memberWithdrawalLogRepository.save(any())
                    deleteMemberSajusPort.deleteByMemberId(MemberFixture.MEMBER_ID)
                    revokeMemberTokensPort.revokeAll(MemberFixture.MEMBER_ID, jti, remainingSeconds)
                    registerWithdrawnAccountPort.register(member.oauthProvider, member.providerId)
                    revokeOauthTokenPort.prepareRevoke(member.oauthProvider, member.providerId)
                    memberRepository.deleteById(MemberFixture.MEMBER_ID)
                }
            }
        }

        context("회원이 존재하지 않으면") {
            it("MemberNotFoundException을 던지고 아무것도 삭제하지 않는다") {
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns null

                shouldThrow<MemberNotFoundException> { service.withdraw(command) }

                verify(exactly = 0) { memberRepository.deleteById(any()) }
                verify(exactly = 0) { deleteMemberSajusPort.deleteByMemberId(any()) }
                verify(exactly = 0) { registerWithdrawnAccountPort.register(any(), any()) }
            }
        }
    }
})
