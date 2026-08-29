package com.yapp.todakun.shared

/**
 * 탈퇴한 회원의 SNS OAuth 토큰을 provider 측에 직접 철회(revoke)하는 크로스 도메인 포트.
 * Apple은 App Store 심사 가이드라인(5.1.1(v))에 따라 계정 삭제 시 Apple 계정 연결도 함께
 * 해제해야 하지만, Kakao/Google은 이런 의무가 없어 provider별로 실제 동작 여부가 갈린다.
 * 탈퇴 시 member가 auth의 OAuth 자격증명 저장소를 직접 알지 않고 이 포트로 위임한다(auth-adapter-out 구현).
 *
 * DB 작업(prepareRevoke)과 외부 API 호출(revoke)을 분리한다: prepareRevoke는 회원 탈퇴 메인
 * 트랜잭션 안에서 호출되어 로컬 자격증명 삭제가 회원 하드 삭제와 원자적으로 묶이고, revoke는 외부
 * HTTP 호출(최대 8초)이라 트랜잭션 커밋 이후 별도로 호출한다.
 */
interface RevokeOauthTokenPort {
    fun prepareRevoke(
        provider: OauthProvider,
        providerId: String,
    ): OauthRevokeCredential?

    fun revoke(credential: OauthRevokeCredential)
}

data class OauthRevokeCredential(
    val providerId: String,
    val clientId: String,
    val refreshToken: String,
)
