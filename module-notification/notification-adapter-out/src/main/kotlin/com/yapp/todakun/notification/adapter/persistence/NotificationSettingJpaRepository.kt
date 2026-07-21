package com.yapp.todakun.notification.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingJpaRepository : JpaRepository<NotificationSettingJpaEntity, UUID> {
    fun findByMemberId(memberId: UUID): NotificationSettingJpaEntity?

    fun findAllByMorningReportEnabledTrueAndMorningReportTime(morningReportTime: LocalTime): List<NotificationSettingJpaEntity>

    fun findAllByLuckyActionReminderEnabledTrue(): List<NotificationSettingJpaEntity>
}
