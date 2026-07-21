package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.NotificationSetting
import java.time.LocalTime
import java.util.UUID

interface GetNotificationSettingUseCase {
    fun getSetting(memberId: UUID): NotificationSetting
}

interface UpdateNotificationSettingUseCase {
    fun update(command: UpdateNotificationSettingCommand): NotificationSetting
}

data class UpdateNotificationSettingCommand(
    val memberId: UUID,
    val morningReportEnabled: Boolean,
    val morningReportTime: LocalTime,
    val todakiEnabled: Boolean,
    val luckyActionReminderEnabled: Boolean,
)
