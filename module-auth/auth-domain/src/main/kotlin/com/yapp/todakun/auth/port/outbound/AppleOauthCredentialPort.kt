package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.auth.AppleOauthCredential

/**
 * Apple SNS 식별자(providerId)별 refresh token 영속 저장소. Apple은 최초 authorization
 * code 교환 시에만 refresh token을 내려주므로, 이후 회원 탈퇴 시 Apple 계정 연결을
 * 해제(revoke)하려면 이 시점에 저장해둔 값을 꺼내 써야 한다.
 */
interface AppleOauthCredentialPort {
    fun save(
        providerId: String,
        clientId: String,
        refreshToken: String,
    )

    fun find(providerId: String): AppleOauthCredential?

    fun delete(providerId: String)
}
