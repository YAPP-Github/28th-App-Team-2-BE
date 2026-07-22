package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import org.springframework.stereotype.Repository
import java.time.LocalTime
import java.util.UUID

@Repository
class NotificationSettingRepositoryAdapter(
    private val notificationSettingJpaRepository: NotificationSettingJpaRepository,
) : NotificationSettingRepository {
    override fun findByMemberId(memberId: UUID): NotificationSetting? =
        notificationSettingJpaRepository.findByMemberId(memberId)?.toDomain()

    override fun save(setting: NotificationSetting): NotificationSetting =
        notificationSettingJpaRepository.save(NotificationSettingJpaEntity.fromDomain(setting)).toDomain()

    override fun findAllMorningReportTargets(slot: LocalTime): List<NotificationSetting> =
        notificationSettingJpaRepository
            .findAllByMorningReportEnabledTrueAndMorningReportTime(slot)
            .map { it.toDomain() }

    override fun findAllLuckyActionReminderTargets(): List<NotificationSetting> =
        notificationSettingJpaRepository.findAllByLuckyActionReminderEnabledTrue().map { it.toDomain() }
}
