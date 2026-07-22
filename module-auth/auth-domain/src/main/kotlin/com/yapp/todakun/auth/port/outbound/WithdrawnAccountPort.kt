package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.shared.OauthProvider

/**
 * 탈퇴한 SNS 로그인 식별자의 재가입 제한 여부를 조회하는 아웃바운드 포트.
 * 로그인 시 신규 회원 분기에서 이 포트로 제한 여부를 확인한다(auth-adapter-out의 Redis 어댑터가 구현).
 */
interface WithdrawnAccountPort {
    /** 해당 SNS 식별자가 재가입 제한 기간(탈퇴 후 90일) 내에 있으면 true. */
    fun isRestricted(
        provider: OauthProvider,
        providerId: String,
    ): Boolean
}
