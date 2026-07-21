package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.DeviceToken
import java.util.UUID

interface DeviceTokenRepository {
    fun findAllByMemberId(memberId: UUID): List<DeviceToken>

    fun findByToken(token: String): DeviceToken?

    fun save(deviceToken: DeviceToken): DeviceToken

    fun deleteByToken(token: String)
}
