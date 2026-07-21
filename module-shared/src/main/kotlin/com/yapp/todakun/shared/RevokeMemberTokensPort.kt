package com.yapp.todakun.shared

import java.util.UUID

/**
 * 회원의 모든 인증 토큰을 폐기하는 크로스 도메인 포트. refresh token은 전부 폐기하고,
 * 탈퇴 요청에 사용된 [accessToken]은 남은 만료 시간 동안 블랙리스트에 등록해 즉시 무효화한다
 * (하드 삭제 후 만료 전까지 탈취 토큰이 재사용되는 창을 차단).
 * 탈퇴 시 member가 auth의 토큰 저장소를 직접 알지 않고 이 포트로 위임한다(auth-adapter-out 구현).
 */
interface RevokeMemberTokensPort {
    fun revokeAll(
        memberId: UUID,
        accessToken: String,
    )
}
