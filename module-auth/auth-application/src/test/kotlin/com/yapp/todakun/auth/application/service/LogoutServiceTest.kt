package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.fixture.AuthFixture
import com.yapp.todakun.auth.fixture.TokenFixture
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class LogoutServiceTest :
    DescribeSpec(
        {
            val accessTokenPort = mockk<AccessTokenPort>()
            val refreshTokenPort = mockk<RefreshTokenPort>()
            val blacklistTokenPort = mockk<BlacklistTokenPort>()
            val logoutService =
                LogoutService(
                    accessTokenPort = accessTokenPort,
                    refreshTokenPort = refreshTokenPort,
                    blacklistTokenPort = blacklistTokenPort,
                )

            afterTest { clearMocks(accessTokenPort, refreshTokenPort, blacklistTokenPort) }

            val command = AuthFixture.logoutCommand()
            val claims = TokenFixture.accessTokenClaims()

            describe("logout") {
                context("accessToken이 유효하면") {
                    it("accessToken을 파싱해 해당 회원의 refreshToken을 모두 폐기하고, jti와 ttl을 블랙리스트에 등록한다") {
                        every { accessTokenPort.parse(command.accessToken) } returns claims
                        every { refreshTokenPort.revokeAll(claims.memberId) } just runs
                        every { blacklistTokenPort.blacklist(claims.jti, claims.remainingSeconds) } just runs

                        logoutService.logout(command)

                        verify(exactly = 1) { refreshTokenPort.revokeAll(claims.memberId) }
                        verify(exactly = 1) { blacklistTokenPort.blacklist(claims.jti, claims.remainingSeconds) }
                    }
                }
            }
        },
    )
