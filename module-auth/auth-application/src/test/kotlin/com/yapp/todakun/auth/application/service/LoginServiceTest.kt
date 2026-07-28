package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.ReSignupRestrictedException
import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.auth.fixture.TokenFixture
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.auth.port.outbound.WithdrawnAccountPort
import com.yapp.todakun.shared.GetMemberIdPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class LoginServiceTest :
    DescribeSpec(
        {
            val oauthPort = mockk<OauthPort>()
            val getMemberIdPort = mockk<GetMemberIdPort>()
            val accessTokenPort = mockk<AccessTokenPort>()
            val refreshTokenPort = mockk<RefreshTokenPort>()
            val onboardingTokenPort = mockk<OnboardingTokenPort>()
            val withdrawnAccountPort = mockk<WithdrawnAccountPort>()
            val loginService =
                LoginService(
                    oauthPort = oauthPort,
                    getMemberIdPort = getMemberIdPort,
                    accessTokenPort = accessTokenPort,
                    refreshTokenPort = refreshTokenPort,
                    onboardingTokenPort = onboardingTokenPort,
                    withdrawnAccountPort = withdrawnAccountPort,
                )

            afterTest {
                clearMocks(oauthPort, getMemberIdPort, accessTokenPort, refreshTokenPort, onboardingTokenPort, withdrawnAccountPort)
            }

            val memberId = AuthFixture.MEMBER_ID
            val command = AuthFixture.loginCommand()
            val profile = AuthFixture.oauthMemberProfile(provider = command.provider)

            val issuedAccessToken = TokenFixture.issuedAccessToken()
            val issuedRefreshToken = TokenFixture.issuedRefreshToken()
            val issuedOnboardingToken = TokenFixture.issuedOnboardingToken()

            describe("login") {
                context("OauthPort.fetchProfile이 올바른 provider와 oauthAccessToken으로 호출되면") {
                    it("OauthMemberProfile을 가져온다") {
                        every { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) } returns profile
                        every { getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) } returns memberId
                        every { accessTokenPort.generate(memberId) } returns issuedAccessToken
                        every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken

                        loginService.login(command)

                        verify(exactly = 1) { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) }
                    }
                }

                context("기존 회원이면") {
                    it("isNewMember = false이고 accessToken과 refreshToken이 채워지며 onboardingToken은 null이다") {
                        every { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) } returns profile
                        every { getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) } returns memberId
                        every { accessTokenPort.generate(memberId) } returns issuedAccessToken
                        every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken

                        val result = loginService.login(command)

                        result.isNewMember shouldBe false
                        result.accessToken shouldBe issuedAccessToken
                        result.refreshToken shouldBe issuedRefreshToken
                        result.onboardingToken.shouldBeNull()
                        verify(exactly = 1) { accessTokenPort.generate(memberId) }
                        verify(exactly = 1) { refreshTokenPort.issue(memberId) }
                        verify(exactly = 0) { onboardingTokenPort.issue(any()) }
                        // 기존 회원은 재가입 제한 검사를 거치지 않는다.
                        verify(exactly = 0) { withdrawnAccountPort.isRestricted(any(), any()) }
                    }
                }

                context("신규 회원이고 재가입 제한 대상이 아니면") {
                    it("isNewMember = true이고 onboardingToken이 채워지며 accessToken과 refreshToken은 null이다") {
                        every { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) } returns profile
                        every { getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) } returns null
                        every { withdrawnAccountPort.isRestricted(profile.provider, profile.providerId) } returns false
                        every { onboardingTokenPort.issue(profile) } returns issuedOnboardingToken

                        val result = loginService.login(command)

                        result.isNewMember shouldBe true
                        result.onboardingToken shouldBe issuedOnboardingToken
                        result.accessToken.shouldBeNull()
                        result.refreshToken.shouldBeNull()
                        verify(exactly = 1) { onboardingTokenPort.issue(profile) }
                        verify(exactly = 0) { accessTokenPort.generate(any()) }
                        verify(exactly = 0) { refreshTokenPort.issue(any()) }
                    }
                }

                context("신규 회원이지만 탈퇴 후 재가입 제한 기간(90일) 내이면") {
                    it("ReSignupRestrictedException을 던지고 온보딩 토큰을 발급하지 않는다") {
                        every { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) } returns profile
                        every { getMemberIdPort.findIdByOauth(profile.provider, profile.providerId) } returns null
                        every { withdrawnAccountPort.isRestricted(profile.provider, profile.providerId) } returns true

                        shouldThrow<ReSignupRestrictedException> { loginService.login(command) }

                        verify(exactly = 0) { onboardingTokenPort.issue(any()) }
                    }
                }
            }
        },
    )
