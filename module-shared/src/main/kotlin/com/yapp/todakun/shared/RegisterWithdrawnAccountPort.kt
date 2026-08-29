package com.yapp.todakun.shared

/**
 * 탈퇴한 회원의 SNS 로그인 식별자를 재가입 제한 목록에 등록하는 크로스 도메인 포트.
 * 탈퇴 시 member가 auth의 식별자 저장소를 직접 알지 않고 이 포트로 위임한다(auth-adapter-out 구현).
 * 등록된 식별자는 정책상 90일간 보관되어 동일 SNS 계정의 재가입을 차단하며, 기간이 지나면 자동 만료된다.
 */
interface RegisterWithdrawnAccountPort {
    fun register(
        provider: OauthProvider,
        providerId: String,
    )
}
