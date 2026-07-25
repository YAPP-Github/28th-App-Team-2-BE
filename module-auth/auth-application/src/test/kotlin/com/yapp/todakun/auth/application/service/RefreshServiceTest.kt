package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.RefreshTokenInvalidException
import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.auth.fixture.TokenFixture
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class RefreshServiceTest :
    DescribeSpec(
        {
            val accessTokenPort = mockk<AccessTokenPort>()
            val refreshTokenPort = mockk<RefreshTokenPort>()
            val refreshService =
                RefreshService(
                    accessTokenPort = accessTokenPort,
                    refreshTokenPort = refreshTokenPort,
                )

            afterTest { clearMocks(accessTokenPort, refreshTokenPort) }

            val command = AuthFixture.refreshCommand()
            val memberId = AuthFixture.MEMBER_ID
            val issuedAccessToken = TokenFixture.issuedAccessToken()
            val issuedRefreshToken = TokenFixture.issuedRefreshToken()

            describe("refresh") {
                context("refreshToken이 유효하면") {
                    it("기존 refreshToken을 폐기하고 access/refresh 토큰을 새로 발급한다") {
                        every { refreshTokenPort.findMemberId(command.refreshToken) } returns memberId
                        every { refreshTokenPort.revoke(command.refreshToken) } just runs
                        every { accessTokenPort.generate(memberId) } returns issuedAccessToken
                        every { refreshTokenPort.issue(memberId) } returns issuedRefreshToken

                        val result = refreshService.refresh(command)

                        result.accessToken shouldBe issuedAccessToken
                        result.refreshToken shouldBe issuedRefreshToken
                        verify(exactly = 1) { refreshTokenPort.revoke(command.refreshToken) }
                    }
                }

                context("refreshToken이 유효하지 않으면") {
                    it("RefreshTokenInvalidException을 던진다") {
                        every { refreshTokenPort.findMemberId(command.refreshToken) } returns null

                        shouldThrow<RefreshTokenInvalidException> { refreshService.refresh(command) }

                        verify(exactly = 0) { refreshTokenPort.revoke(any()) }
                    }
                }
            }
        },
    )
