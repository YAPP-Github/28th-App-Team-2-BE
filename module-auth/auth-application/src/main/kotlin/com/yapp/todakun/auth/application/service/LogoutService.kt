package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.port.inbound.LogoutCommand
import com.yapp.todakun.auth.port.inbound.LogoutUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.annotation.CommandService

@CommandService
class LogoutService(
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
    private val blacklistTokenPort: BlacklistTokenPort,
) : LogoutUseCase {
    override fun logout(command: LogoutCommand) {
        val claims = accessTokenPort.parse(command.accessToken)

        refreshTokenPort.revokeAll(claims.memberId)
        blacklistTokenPort.blacklist(claims.jti, claims.remainingSeconds)
    }
}
