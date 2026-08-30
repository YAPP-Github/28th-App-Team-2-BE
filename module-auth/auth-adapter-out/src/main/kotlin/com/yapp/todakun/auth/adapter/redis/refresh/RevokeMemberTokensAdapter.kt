package com.yapp.todakun.auth.adapter.redis.refresh

import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.shared.RevokeMemberTokensPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 회원 토큰 폐기 크로스 도메인 포트([RevokeMemberTokensPort]) 구현. 탈퇴 시 member가 이 포트로 위임하면
 * 해당 회원의 모든 refresh token을 폐기하고, 탈퇴에 사용된 access token은 남은 만료 시간(jti·remainingSeconds,
 * 인증 필터가 이미 검증해 둔 값)만큼 블랙리스트에 등록해 즉시 무효화한다(로그아웃과 동일한 폐기 방식).
 */
@Component
class RevokeMemberTokensAdapter(
    private val refreshTokenPort: RefreshTokenPort,
    private val blacklistTokenPort: BlacklistTokenPort,
) : RevokeMemberTokensPort {
    override fun revokeAll(
        memberId: UUID,
        jti: String,
        remainingSeconds: Long,
    ) {
        refreshTokenPort.revokeAll(memberId)
        blacklistTokenPort.blacklist(jti, remainingSeconds)
    }
}
