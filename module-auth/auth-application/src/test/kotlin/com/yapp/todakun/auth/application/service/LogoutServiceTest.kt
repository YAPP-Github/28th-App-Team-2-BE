package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.fixture.AuthFixture
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
            val refreshTokenPort = mockk<RefreshTokenPort>()
            val blacklistTokenPort = mockk<BlacklistTokenPort>()
            val logoutService =
                LogoutService(
                    refreshTokenPort = refreshTokenPort,
                    blacklistTokenPort = blacklistTokenPort,
                )

            afterTest { clearMocks(refreshTokenPort, blacklistTokenPort) }

            val command = AuthFixture.logoutCommand()

            describe("logout") {
                context("로그아웃 요청이 오면") {
                    it("해당 회원의 refreshToken을 모두 폐기하고, jti와 ttl을 블랙리스트에 등록한다") {
                        every { refreshTokenPort.revokeAll(command.memberId) } just runs
                        every { blacklistTokenPort.blacklist(command.jti, command.remainingSeconds) } just runs

                        logoutService.logout(command)

                        verify(exactly = 1) { refreshTokenPort.revokeAll(command.memberId) }
                        verify(exactly = 1) { blacklistTokenPort.blacklist(command.jti, command.remainingSeconds) }
                    }
                }
            }
        },
    )
