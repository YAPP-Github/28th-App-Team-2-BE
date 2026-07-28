package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.OnboardingTokenInvalidException
import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.auth.fixture.TokenFixture
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.CreateMemberPort
import com.yapp.todakun.shared.CreateSajuChartPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

private val DAILY_FORTUNE_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000006")

class SignupServiceTest :
    DescribeSpec(
        {
            val onboardingTokenPort = mockk<OnboardingTokenPort>()
            val createMemberPort = mockk<CreateMemberPort>()
            val createSajuChartPort = mockk<CreateSajuChartPort>()
            val createDailyFortunePort = mockk<CreateDailyFortunePort>()
            val accessTokenPort = mockk<AccessTokenPort>()
            val refreshTokenPort = mockk<RefreshTokenPort>()
            val signupService =
                SignupService(
                    onboardingTokenPort = onboardingTokenPort,
                    createMemberPort = createMemberPort,
                    createSajuChartPort = createSajuChartPort,
                    createDailyFortunePort = createDailyFortunePort,
                    accessTokenPort = accessTokenPort,
                    refreshTokenPort = refreshTokenPort,
                )

            afterTest {
                clearMocks(
                    onboardingTokenPort,
                    createMemberPort,
                    createSajuChartPort,
                    createDailyFortunePort,
                    accessTokenPort,
                    refreshTokenPort,
                )
            }

            val memberId = AuthFixture.MEMBER_ID
            val command = AuthFixture.signupCommand()
            val profile = AuthFixture.oauthMemberProfile()

            val issuedAccessToken = TokenFixture.issuedAccessToken()
            val issuedRefreshToken = TokenFixture.issuedRefreshToken()

            describe("signup") {
                context("onboardingToken이 유효하지 않으면") {
                    it("OnboardingTokenInvalidException을 던진다") {
                        every { onboardingTokenPort.findProfile(command.onboardingToken) } returns null

                        shouldThrow<OnboardingTokenInvalidException> { signupService.signup(command) }

                        verify(
                            exactly = 0,
                        ) { createMemberPort.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
                        verify(exactly = 0) { createDailyFortunePort.create(any(), any()) }
                    }
                }

                context("onboardingToken이 유효하면") {
                    it("회원을 생성하고 accessToken과 refreshToken을 발급하며 onboardingToken을 폐기한다") {
                        every { onboardingTokenPort.findProfile(command.onboardingToken) } returns profile
                        every {
                            createMemberPort.create(
                                provider = profile.provider,
                                providerId = profile.providerId,
                                name = command.name,
                                birthDate = command.birthDate,
                                birthTime = command.birthTime,
                                calendarType = command.calendarType,
                                gender = command.gender,
                                job = command.job,
                                relationshipStatus = command.relationshipStatus,
                                favoriteFortuneCategories = command.favoriteFortuneCategories,
                            )
                        } returns memberId
                        every {
                            createSajuChartPort.create(
                                memberId = memberId,
                                isSelf = true,
                                name = command.name,
                                gender = command.gender,
                                calendarType = command.calendarType,
                                birthDate = command.birthDate,
                                birthTime = command.birthTime,
                                isLeapMonth = false,
                            )
                        } returns memberId
                        every { createDailyFortunePort.create(memberId = memberId, fortuneDate = any()) } returns DAILY_FORTUNE_ID
                        every { accessTokenPort.generate(memberId) } returns issuedAccessToken
                        every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken
                        every { onboardingTokenPort.revoke(command.onboardingToken) } just Runs

                        val result = signupService.signup(command)

                        result.accessToken shouldBe issuedAccessToken
                        result.refreshToken shouldBe issuedRefreshToken
                        verify(exactly = 1) {
                            createSajuChartPort.create(
                                memberId = memberId,
                                isSelf = true,
                                name = command.name,
                                gender = command.gender,
                                calendarType = command.calendarType,
                                birthDate = command.birthDate,
                                birthTime = command.birthTime,
                                isLeapMonth = false,
                            )
                        }
                        verify(exactly = 1) { createDailyFortunePort.create(memberId = memberId, fortuneDate = any()) }
                        verify(exactly = 1) { onboardingTokenPort.revoke(command.onboardingToken) }
                    }
                }
            }
        },
    )
