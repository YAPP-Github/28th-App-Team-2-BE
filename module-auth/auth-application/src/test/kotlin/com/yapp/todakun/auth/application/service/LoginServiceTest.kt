package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.auth.fixture.TokenFixture
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.shared.MemberAuthPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class LoginServiceTest :
    DescribeSpec({
        val oauthPort = mockk<OauthPort>()
        val memberAuthPort = mockk<MemberAuthPort>()
        val accessTokenPort = mockk<AccessTokenPort>()
        val refreshTokenPort = mockk<RefreshTokenPort>()
        val onboardingTokenPort = mockk<OnboardingTokenPort>()
        val loginService =
            LoginService(
                oauthPort = oauthPort,
                memberAuthPort = memberAuthPort,
                accessTokenPort = accessTokenPort,
                refreshTokenPort = refreshTokenPort,
                onboardingTokenPort = onboardingTokenPort,
            )

        afterTest { clearMocks(oauthPort, memberAuthPort, accessTokenPort, refreshTokenPort, onboardingTokenPort) }

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
                    every { memberAuthPort.findMemberId(profile.provider, profile.providerId) } returns memberId
                    every { accessTokenPort.generate(memberId) } returns issuedAccessToken
                    every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken

                    loginService.login(command)

                    verify(exactly = 1) { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) }
                }
            }

            context("기존 회원이면") {
                it("isNewMember = false이고 accessToken과 refreshToken이 채워지며 onboardingToken은 null이다") {
                    every { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) } returns profile
                    every { memberAuthPort.findMemberId(profile.provider, profile.providerId) } returns memberId
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
                }
            }

            context("신규 회원이면") {
                it("isNewMember = true이고 onboardingToken이 채워지며 accessToken과 refreshToken은 null이다") {
                    every { oauthPort.fetchProfile(command.provider, command.oauthAccessToken) } returns profile
                    every { memberAuthPort.findMemberId(profile.provider, profile.providerId) } returns null
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
        }
    })
