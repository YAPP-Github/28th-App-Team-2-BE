package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import java.time.LocalTime
import java.util.UUID

@Repository
class NotificationSettingRepositoryAdapter(
    private val notificationSettingJpaRepository: NotificationSettingJpaRepository,
) : NotificationSettingRepository {
    override fun findByMemberId(memberId: UUID): NotificationSetting? =
        notificationSettingJpaRepository.findByMemberId(memberId)?.toDomain()

    // member_id 기준 upsert(단일 원자적 statement)라 동시 최초 생성 요청 경합에도 예외 없이 안전하다.
    override fun save(setting: NotificationSetting): NotificationSetting {
        notificationSettingJpaRepository.upsert(
            id = setting.id,
            memberId = setting.memberId,
            morningReportEnabled = setting.morningReportEnabled,
            morningReportTime = setting.morningReportTime,
            todakiEnabled = setting.todakiEnabled,
            luckyActionReminderEnabled = setting.luckyActionReminderEnabled,
            osPushPermission = setting.osPushPermission,
        )
        return requireNotNull(findByMemberId(setting.memberId))
    }

    override fun findMorningReportTargets(
        slot: LocalTime,
        afterId: UUID?,
        limit: Int,
    ): List<NotificationSetting> =
        notificationSettingJpaRepository
            .findMorningReportTargets(slot, afterId, Limit.of(limit))
            .map { it.toDomain() }

    override fun findLuckyActionReminderTargets(
        afterId: UUID?,
        limit: Int,
    ): List<NotificationSetting> =
        notificationSettingJpaRepository.findLuckyActionReminderTargets(afterId, Limit.of(limit)).map { it.toDomain() }
}
