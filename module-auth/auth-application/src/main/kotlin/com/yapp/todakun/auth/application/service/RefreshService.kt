package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.RefreshTokenInvalidException
import com.yapp.todakun.auth.port.inbound.RefreshCommand
import com.yapp.todakun.auth.port.inbound.RefreshResult
import com.yapp.todakun.auth.port.inbound.RefreshUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.annotation.CommandService

@CommandService
class RefreshService(
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
) : RefreshUseCase {
    override fun refresh(command: RefreshCommand): RefreshResult {
        val memberId = refreshTokenPort.consume(command.refreshToken) ?: throw RefreshTokenInvalidException()

        return RefreshResult(
            accessToken = accessTokenPort.generate(memberId),
            refreshToken = refreshTokenPort.issue(memberId),
        )
    }
}
