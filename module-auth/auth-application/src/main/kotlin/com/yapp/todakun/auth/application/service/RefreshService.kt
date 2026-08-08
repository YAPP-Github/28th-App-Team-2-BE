package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.RefreshTokenInvalidException
import com.yapp.todakun.auth.port.inbound.RefreshCommand
import com.yapp.todakun.auth.port.inbound.RefreshResult
import com.yapp.todakun.auth.port.inbound.RefreshUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.IsAdminMemberPort

@CommandService
class RefreshService(
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
    private val isAdminMemberPort: IsAdminMemberPort,
) : RefreshUseCase {
    override fun refresh(command: RefreshCommand): RefreshResult {
        val memberId = refreshTokenPort.consume(command.refreshToken) ?: throw RefreshTokenInvalidException()

        return RefreshResult(
            // 매 refresh마다 role을 다시 조회한다 — 로그인 이후 DB에서 승격/강등됐을 수 있어서다.
            accessToken = accessTokenPort.generate(memberId, isAdminMemberPort.isAdmin(memberId)),
            refreshToken = refreshTokenPort.issue(memberId),
        )
    }
}
