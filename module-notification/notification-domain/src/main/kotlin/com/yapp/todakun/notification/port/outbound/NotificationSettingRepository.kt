package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.NotificationSetting
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingRepository {
    fun findByMemberId(memberId: UUID): NotificationSetting?

    fun save(setting: NotificationSetting): NotificationSetting

    /** 아침 운 리포트가 켜져 있고 받을 시간이 [slot]인 회원 설정. */
    fun findAllMorningReportTargets(slot: LocalTime): List<NotificationSetting>

    /** 행운 액션 리마인드가 켜져 있는 회원 설정. */
    fun findAllLuckyActionReminderTargets(): List<NotificationSetting>
}
