package com.yapp.todakun.auth

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * Apple SNS 식별자([providerId])별로 발급받은 refresh token 보관 기록.
 * 탈퇴 시 Apple 계정 연결을 해제(revoke)하기 위해 로그인 시점마다 최신 값으로 갱신해둔다.
 */
data class AppleOauthCredential(
    val id: UUID,
    val providerId: String,
    val clientId: String,
    val refreshToken: String,
) {
    companion object {
        @ExperimentalUuidApi
        fun create(
            providerId: String,
            clientId: String,
            refreshToken: String,
        ): AppleOauthCredential =
            AppleOauthCredential(
                id = Uuid.generateV7().toJavaUuid(),
                providerId = providerId,
                clientId = clientId,
                refreshToken = refreshToken,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            providerId: String,
            clientId: String,
            refreshToken: String,
        ): AppleOauthCredential =
            AppleOauthCredential(
                id = id,
                providerId = providerId,
                clientId = clientId,
                refreshToken = refreshToken,
            )
    }
}
