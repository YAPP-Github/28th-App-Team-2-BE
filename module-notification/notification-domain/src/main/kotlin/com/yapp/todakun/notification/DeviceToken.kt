package com.yapp.todakun.notification

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 회원 기기의 FCM 등록 토큰. 토큰은 유일하며, 재로그인 등으로 기기가 다른 회원에 재배정되면
 * [reassign]으로 소유 회원을 갱신한다(토큰 자체는 유지).
 */
data class DeviceToken(
    val id: UUID,
    val memberId: UUID,
    val token: String,
    val platform: Platform,
) {
    fun reassign(
        memberId: UUID,
        platform: Platform,
    ): DeviceToken = copy(memberId = memberId, platform = platform)

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            memberId: UUID,
            token: String,
            platform: Platform,
        ): DeviceToken =
            DeviceToken(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                token = token,
                platform = platform,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            token: String,
            platform: Platform,
        ): DeviceToken = DeviceToken(id, memberId, token, platform)
    }
}
