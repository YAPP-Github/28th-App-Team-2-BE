package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import org.springframework.dao.DataIntegrityViolationException
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

    override fun save(setting: NotificationSetting): NotificationSetting =
        try {
            notificationSettingJpaRepository.save(NotificationSettingJpaEntity.fromDomain(setting)).toDomain()
        } catch (_: DataIntegrityViolationException) {
            // 최초 생성 시 동시 요청 경합(member_id 유니크 제약) — 먼저 커밋된 행의 id로 갱신을 재시도한다.
            val existingId = requireNotNull(notificationSettingJpaRepository.findByMemberId(setting.memberId)).id
            notificationSettingJpaRepository.save(NotificationSettingJpaEntity.fromDomain(setting.copy(id = existingId))).toDomain()
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
