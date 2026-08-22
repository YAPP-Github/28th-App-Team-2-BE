package com.yapp.todakun.member.application.service

import com.yapp.todakun.member.WithdrawalReason
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.port.inbound.WithdrawMemberCommand
import com.yapp.todakun.shared.OauthRevokeCredential
import com.yapp.todakun.shared.RevokeOauthTokenPort
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
class WithdrawMemberServiceTest : DescribeSpec({
    val withdrawMemberTransactionService = mockk<WithdrawMemberTransactionService>()
    val revokeOauthTokenPort = mockk<RevokeOauthTokenPort>()
    val service = WithdrawMemberService(withdrawMemberTransactionService, revokeOauthTokenPort)

    afterTest {
        clearMocks(withdrawMemberTransactionService, revokeOauthTokenPort)
    }

    val accessToken = "test-access-token"
    val command = WithdrawMemberCommand(MemberFixture.MEMBER_ID, WithdrawalReason.LOW_USAGE, detail = null, accessToken = accessToken)

    describe("withdraw") {
        context("트랜잭션 처리가 성공하고 revoke할 OAuth 자격증명이 있으면") {
            it("커밋 이후 반환된 자격증명으로 OAuth 토큰 철회를 호출한다") {
                val oauthRevokeCredential = OauthRevokeCredential(MemberFixture.member().providerId, "client-id", "refresh-token")
                every { withdrawMemberTransactionService.withdraw(command) } returns oauthRevokeCredential
                every { revokeOauthTokenPort.revoke(oauthRevokeCredential) } just Runs

                service.withdraw(command)

                verifyOrder {
                    withdrawMemberTransactionService.withdraw(command)
                    revokeOauthTokenPort.revoke(oauthRevokeCredential)
                }
            }
        }

        context("트랜잭션 처리가 성공했지만 revoke할 OAuth 자격증명이 없으면") {
            it("OAuth 토큰 철회를 호출하지 않는다") {
                every { withdrawMemberTransactionService.withdraw(command) } returns null

                service.withdraw(command)

                verify(exactly = 0) { revokeOauthTokenPort.revoke(any()) }
            }
        }

        context("트랜잭션 처리 중 회원을 찾지 못하면") {
            it("MemberNotFoundException을 전파하고 OAuth 토큰 철회를 호출하지 않는다") {
                every { withdrawMemberTransactionService.withdraw(command) } throws MemberNotFoundException()

                shouldThrow<MemberNotFoundException> { service.withdraw(command) }

                verify(exactly = 0) { revokeOauthTokenPort.revoke(any()) }
            }
        }
    }
})
