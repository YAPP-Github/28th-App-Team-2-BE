package com.yapp.todakun.shared

import java.util.UUID

/**
 * 회원의 모든 인증 토큰(refresh token 등)을 폐기하는 크로스 도메인 포트.
 * 탈퇴 시 member가 auth의 토큰 저장소를 직접 알지 않고 이 포트로 위임한다(auth-adapter-out 구현).
 */
interface RevokeMemberTokensPort {
    fun revokeAll(memberId: UUID)
}
