package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.DeviceToken
import com.yapp.todakun.notification.port.outbound.DeviceTokenRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DeviceTokenRepositoryAdapter(
    private val deviceTokenJpaRepository: DeviceTokenJpaRepository,
) : DeviceTokenRepository {
    override fun findAllByMemberId(memberId: UUID): List<DeviceToken> =
        deviceTokenJpaRepository.findAllByMemberId(memberId).map { it.toDomain() }

    override fun findByToken(token: String): DeviceToken? = deviceTokenJpaRepository.findByToken(token)?.toDomain()

    override fun save(deviceToken: DeviceToken): DeviceToken =
        deviceTokenJpaRepository.save(DeviceTokenJpaEntity.fromDomain(deviceToken)).toDomain()

    override fun deleteByToken(token: String) = deviceTokenJpaRepository.deleteByToken(token)
}
