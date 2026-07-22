package com.yapp.todakun.notification.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeviceTokenJpaRepository : JpaRepository<DeviceTokenJpaEntity, UUID> {
    fun findAllByMemberId(memberId: UUID): List<DeviceTokenJpaEntity>

    fun findByToken(token: String): DeviceTokenJpaEntity?

    fun deleteByToken(token: String)

    fun deleteByMemberIdAndToken(
        memberId: UUID,
        token: String,
    )
}
