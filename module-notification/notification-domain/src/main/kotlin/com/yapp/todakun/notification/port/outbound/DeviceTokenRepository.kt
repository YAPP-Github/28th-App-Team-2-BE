package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.DeviceToken
import java.util.UUID

interface DeviceTokenRepository {
    fun findAllByMemberId(memberId: UUID): List<DeviceToken>

    fun findByToken(token: String): DeviceToken?

    fun save(deviceToken: DeviceToken): DeviceToken

    /** 발송 결과로 확인된 만료 토큰 정리용 — 토큰은 전역 유일하므로 토큰만으로 삭제한다. */
    fun deleteByToken(token: String)

    /** 사용자 요청(로그아웃)에 의한 해제 — 타인 토큰 삭제를 막기 위해 회원 소유 범위로 제한한다. */
    fun deleteByMemberIdAndToken(
        memberId: UUID,
        token: String,
    )
}
