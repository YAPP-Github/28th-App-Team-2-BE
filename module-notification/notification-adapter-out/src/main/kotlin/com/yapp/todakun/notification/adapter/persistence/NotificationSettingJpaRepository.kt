package com.yapp.todakun.notification.adapter.persistence

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingJpaRepository : JpaRepository<NotificationSettingJpaEntity, UUID> {
    fun findByMemberId(memberId: UUID): NotificationSettingJpaEntity?

    // id(UUIDv7, 시간 정렬)를 keyset 커서로 써서 OFFSET 없이 다음 페이지를 조회한다.
    @Query(
        "select n from NotificationSettingJpaEntity n " +
            "where n.morningReportEnabled = true and n.morningReportTime = :morningReportTime " +
            "and (:afterId is null or n.id > :afterId) order by n.id asc",
    )
    fun findMorningReportTargets(
        morningReportTime: LocalTime,
        afterId: UUID?,
        limit: Limit,
    ): List<NotificationSettingJpaEntity>

    @Query(
        "select n from NotificationSettingJpaEntity n " +
            "where n.luckyActionReminderEnabled = true and (:afterId is null or n.id > :afterId) order by n.id asc",
    )
    fun findLuckyActionReminderTargets(
        afterId: UUID?,
        limit: Limit,
    ): List<NotificationSettingJpaEntity>
}
