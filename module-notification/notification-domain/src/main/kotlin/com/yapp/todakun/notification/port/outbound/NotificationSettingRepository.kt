package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.NotificationSetting
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingRepository {
    fun findByMemberId(memberId: UUID): NotificationSetting?

    fun save(setting: NotificationSetting): NotificationSetting

    /**
     * 아침 운 리포트가 켜져 있고 받을 시간이 [slot]인 회원 설정을, id(UUIDv7) 기준 keyset 페이징으로 조회한다.
     * [afterId] 이후(오름차순) [limit]개를 반환하며, null이면 처음부터 조회한다.
     */
    fun findMorningReportTargets(
        slot: LocalTime,
        afterId: UUID?,
        limit: Int,
    ): List<NotificationSetting>

    /** 행운 액션 리마인드가 켜져 있는 회원 설정을, id(UUIDv7) 기준 keyset 페이징으로 조회한다. */
    fun findLuckyActionReminderTargets(
        afterId: UUID?,
        limit: Int,
    ): List<NotificationSetting>
}
