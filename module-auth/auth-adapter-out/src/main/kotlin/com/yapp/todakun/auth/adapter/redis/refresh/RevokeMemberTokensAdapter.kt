package com.yapp.todakun.auth.adapter.redis.refresh

import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.shared.RevokeMemberTokensPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 회원 토큰 폐기 크로스 도메인 포트([RevokeMemberTokensPort]) 구현. 탈퇴 시 member가 이 포트로 위임하면
 * 해당 회원의 모든 refresh token을 폐기한다(access token은 짧은 만료로 자연 소멸).
 */
@Component
class RevokeMemberTokensAdapter(
    private val refreshTokenPort: RefreshTokenPort,
) : RevokeMemberTokensPort {
    override fun revokeAll(memberId: UUID) = refreshTokenPort.revokeAll(memberId)
}
