package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.port.inbound.LogoutCommand
import com.yapp.todakun.auth.port.inbound.LogoutUseCase
import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import org.springframework.stereotype.Service

// 사용하는 포트가 모두 Redis이고 RDB 트랜잭션이 필요 없어 @CommandService(트랜잭션) 대신 @Service를 사용한다.
@Service
class LogoutService(
    private val refreshTokenPort: RefreshTokenPort,
    private val blacklistTokenPort: BlacklistTokenPort,
) : LogoutUseCase {
    override fun logout(command: LogoutCommand) {
        refreshTokenPort.revokeAll(command.memberId)
        blacklistTokenPort.blacklist(command.jti, command.remainingSeconds)
    }
}
