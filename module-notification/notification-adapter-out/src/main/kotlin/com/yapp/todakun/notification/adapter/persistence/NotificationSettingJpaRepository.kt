package com.yapp.todakun.notification.adapter.persistence

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingJpaRepository : JpaRepository<NotificationSettingJpaEntity, UUID> {
    fun findByMemberId(memberId: UUID): NotificationSettingJpaEntity?

    /**
     * member_id 기준 원자적 upsert. 동시 최초 생성 요청 경합(member_id 유니크 제약 충돌)을
     * 애플리케이션 레벨 재시도 없이 DB가 단일 statement로 해결하게 한다 — Postgres는 제약 위반이
     * 발생하면 트랜잭션을 abort 상태로 만들어, 같은 트랜잭션 안에서 조회/재시도를 하는 방식 자체가
     * 성립하지 않는다.
     * clearAutomatically: 네이티브 upsert는 영속성 컨텍스트를 거치지 않아, 이전에 조회해 캐시된
     * 관리 엔티티가 남아 있으면 이후 재조회 시 stale 값이 반환된다 — 캐시를 비워 다음 조회가 DB를 다시 읽게 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
            insert into notification_setting
                (id, member_id, morning_report_enabled, morning_report_time, todaki_enabled,
                 lucky_action_reminder_enabled, os_push_permission, created_at, updated_at)
            values
                (:id, :memberId, :morningReportEnabled, :morningReportTime, :todakiEnabled,
                 :luckyActionReminderEnabled, :osPushPermission, now(), now())
            on conflict (member_id) do update set
                morning_report_enabled = excluded.morning_report_enabled,
                morning_report_time = excluded.morning_report_time,
                todaki_enabled = excluded.todaki_enabled,
                lucky_action_reminder_enabled = excluded.lucky_action_reminder_enabled,
                os_push_permission = excluded.os_push_permission,
                updated_at = now()
            """,
    )
    fun upsert(
        id: UUID,
        memberId: UUID,
        morningReportEnabled: Boolean,
        morningReportTime: LocalTime,
        todakiEnabled: Boolean,
        luckyActionReminderEnabled: Boolean,
        osPushPermission: Boolean?,
    )

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
