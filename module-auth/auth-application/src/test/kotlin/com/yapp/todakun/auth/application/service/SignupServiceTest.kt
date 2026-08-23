package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.OnboardingTokenInvalidException
import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.auth.fixture.TokenFixture
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.code.ResponseCode
import com.yapp.todakun.common.exception.ConflictException
import com.yapp.todakun.shared.GetMemberIdPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

/** member-adapter-out이 회원 유니크 제약 위반 시 던지는 예외(MemberAlreadyExistsException)의 대역. */
private object MemberAlreadyExistsCode : ResponseCode {
    override val code = "MEMBER-409"
    override val message = "이미 가입된 회원입니다."
    override val status = 409
}

private class MemberAlreadyExistsStub : ConflictException(MemberAlreadyExistsCode)

class SignupServiceTest :
    DescribeSpec(
        {
            val onboardingTokenPort = mockk<OnboardingTokenPort>()
            val signupTransactionService = mockk<SignupTransactionService>()
            val getMemberIdPort = mockk<GetMemberIdPort>()
            val accessTokenPort = mockk<AccessTokenPort>()
            val refreshTokenPort = mockk<RefreshTokenPort>()
            val signupService =
                SignupService(
                    onboardingTokenPort = onboardingTokenPort,
                    signupTransactionService = signupTransactionService,
                    getMemberIdPort = getMemberIdPort,
                    accessTokenPort = accessTokenPort,
                    refreshTokenPort = refreshTokenPort,
                )

            afterTest {
                clearMocks(
                    onboardingTokenPort,
                    signupTransactionService,
                    getMemberIdPort,
                    accessTokenPort,
                    refreshTokenPort,
                )
            }

            val memberId = AuthFixture.MEMBER_ID
            val command = AuthFixture.signupCommand()
            val profile = AuthFixture.oauthMemberProfile()

            val issuedAccessToken = TokenFixture.issuedAccessToken()
            val issuedRefreshToken = TokenFixture.issuedRefreshToken()

            fun stubTokenIssue() {
                every { accessTokenPort.generate(memberId, false) } returns issuedAccessToken
                every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken
                every { onboardingTokenPort.revoke(command.onboardingToken) } just Runs
            }

            describe("signup") {
                context("onboardingToken이 유효하지 않으면") {
                    it("OnboardingTokenInvalidException을 던지고 아무것도 생성하지 않는다") {
                        every { onboardingTokenPort.findProfile(command.onboardingToken) } returns null

                        shouldThrow<OnboardingTokenInvalidException> { signupService.signup(command) }

                        verify(exactly = 0) { signupTransactionService.register(any(), any()) }
                    }
                }

                context("아직 가입되지 않은 소셜 계정이면") {
                    it("회원·사주를 생성한 뒤 토큰을 발급하고 onboardingToken을 폐기한다") {
                        every { onboardingTokenPort.findProfile(command.onboardingToken) } returns profile
                        every { getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) } returns null
                        every { signupTransactionService.register(profile, command) } returns memberId
                        stubTokenIssue()

                        val result = signupService.signup(command)

                        result.accessToken shouldBe issuedAccessToken
                        result.refreshToken shouldBe issuedRefreshToken
                        verify(exactly = 1) { signupTransactionService.register(profile, command) }
                        verify(exactly = 1) { onboardingTokenPort.revoke(command.onboardingToken) }
                    }
                }

                context("이미 같은 소셜 계정으로 가입돼 있으면(첫 요청 타임아웃 후 재시도)") {
                    it("409 대신 기존 회원의 토큰을 발급하고 회원을 다시 만들지 않는다") {
                        every { onboardingTokenPort.findProfile(command.onboardingToken) } returns profile
                        every { getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) } returns memberId
                        stubTokenIssue()

                        val result = signupService.signup(command)

                        result.accessToken shouldBe issuedAccessToken
                        result.refreshToken shouldBe issuedRefreshToken
                        verify(exactly = 0) { signupTransactionService.register(any(), any()) }
                        verify(exactly = 1) { onboardingTokenPort.revoke(command.onboardingToken) }
                    }
                }

                context("동시 중복 요청과 경합해 회원 생성이 유니크 제약에 걸렸고 상대 요청이 먼저 커밋했으면") {
                    it("그 회원으로 멱등 처리해 토큰을 발급한다") {
                        every { onboardingTokenPort.findProfile(command.onboardingToken) } returns profile
                        every {
                            getMemberIdPort.findIdByOauth(profile.provider, profile.providerId)
                        } returnsMany listOf(null, memberId)
                        every { signupTransactionService.register(profile, command) } throws MemberAlreadyExistsStub()
                        stubTokenIssue()

                        val result = signupService.signup(command)

                        result.accessToken shouldBe issuedAccessToken
                        verify(exactly = 1) { onboardingTokenPort.revoke(command.onboardingToken) }
                    }
                }

                context("회원 생성이 실패했고 해당 소셜 계정의 회원도 없으면") {
                    it("원래 예외를 그대로 던지고 onboardingToken을 폐기하지 않는다(같은 토큰으로 재시도 가능)") {
                        every { onboardingTokenPort.findProfile(command.onboardingToken) } returns profile
                        every { getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) } returns null
                        every { signupTransactionService.register(profile, command) } throws MemberAlreadyExistsStub()

                        shouldThrow<MemberAlreadyExistsStub> { signupService.signup(command) }

                        verify(exactly = 0) { onboardingTokenPort.revoke(any()) }
                    }
                }
            }
        },
    )
