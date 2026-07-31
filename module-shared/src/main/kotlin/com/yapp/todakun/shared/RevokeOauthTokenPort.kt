package com.yapp.todakun.shared

/**
 * 탈퇴한 회원의 SNS OAuth 토큰을 provider 측에 직접 철회(revoke)하는 크로스 도메인 포트.
 * Apple은 App Store 심사 가이드라인(5.1.1(v))에 따라 계정 삭제 시 Apple 계정 연결도 함께
 * 해제해야 하지만, Kakao/Google은 이런 의무가 없어 provider별로 실제 동작 여부가 갈린다.
 * 탈퇴 시 member가 auth의 OAuth 자격증명 저장소를 직접 알지 않고 이 포트로 위임한다(auth-adapter-out 구현).
 */
interface RevokeOauthTokenPort {
    fun revokeIfApplicable(
        provider: OauthProvider,
        providerId: String,
    )
}
