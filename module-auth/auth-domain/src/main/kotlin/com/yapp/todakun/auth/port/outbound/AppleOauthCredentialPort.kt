package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.auth.AppleOauthCredential

/**
 * Apple SNS 식별자(providerId)별 refresh token 영속 저장소.
 * Apple은 로그인마다 매번 새로운 authorization code를 발급하고, 교환 시 refresh token을 내려준다.
 * 이후 회원 탈퇴 시 Apple 계정 연결을 해제(revoke)하려면 가장 최근 로그인 시점에 저장해둔 값을 꺼내서 사용해야 한다.
 */
interface AppleOauthCredentialPort {
    fun save(
        providerId: String,
        clientId: String,
        refreshToken: String,
    ): AppleOauthCredential

    fun find(providerId: String): AppleOauthCredential?

    fun delete(providerId: String)
}
