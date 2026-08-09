package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.RefreshTokenInvalidException
import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.auth.fixture.TokenFixture
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.shared.IsAdminMemberPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RefreshServiceTest :
    DescribeSpec(
        {
            val accessTokenPort = mockk<AccessTokenPort>()
            val refreshTokenPort = mockk<RefreshTokenPort>()
            val isAdminMemberPort = mockk<IsAdminMemberPort>()
            val refreshService =
                RefreshService(
                    accessTokenPort = accessTokenPort,
                    refreshTokenPort = refreshTokenPort,
                    isAdminMemberPort = isAdminMemberPort,
                )

            afterTest { clearMocks(accessTokenPort, refreshTokenPort, isAdminMemberPort) }

            val command = AuthFixture.refreshCommand()
            val memberId = AuthFixture.MEMBER_ID
            val issuedAccessToken = TokenFixture.issuedAccessToken()
            val issuedRefreshToken = TokenFixture.issuedRefreshToken()

            describe("refresh") {
                context("refreshToken이 유효하면") {
                    it("기존 refreshToken을 소비하고 access/refresh 토큰을 새로 발급한다") {
                        every { refreshTokenPort.consume(command.refreshToken) } returns memberId
                        every { isAdminMemberPort.isAdmin(memberId) } returns false
                        every { accessTokenPort.generate(memberId, false) } returns issuedAccessToken
                        every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken

                        val result = refreshService.refresh(command)

                        result.accessToken shouldBe issuedAccessToken
                        result.refreshToken shouldBe issuedRefreshToken
                        verify(exactly = 1) { refreshTokenPort.consume(command.refreshToken) }
                    }
                }

                context("갱신 대상 회원이 관리자(Role.ADMIN)이면") {
                    it("accessToken을 isAdmin=true로 재발급한다") {
                        every { refreshTokenPort.consume(command.refreshToken) } returns memberId
                        every { isAdminMemberPort.isAdmin(memberId) } returns true
                        every { accessTokenPort.generate(memberId, true) } returns issuedAccessToken
                        every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken

                        refreshService.refresh(command)

                        verify(exactly = 1) { accessTokenPort.generate(memberId, true) }
                    }
                }

                context("refreshToken이 유효하지 않거나 이미 소비되었으면") {
                    it("RefreshTokenInvalidException을 던진다") {
                        every { refreshTokenPort.consume(command.refreshToken) } returns null

                        shouldThrow<RefreshTokenInvalidException> { refreshService.refresh(command) }

                        verify(exactly = 0) { refreshTokenPort.issue(any()) }
                    }
                }
            }
        },
    )
