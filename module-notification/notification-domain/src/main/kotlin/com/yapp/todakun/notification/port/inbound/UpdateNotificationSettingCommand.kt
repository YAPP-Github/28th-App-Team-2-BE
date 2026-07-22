package com.yapp.todakun.notification.port.inbound

import java.time.LocalTime
import java.util.UUID

data class UpdateNotificationSettingCommand(
    val memberId: UUID,
    val morningReportEnabled: Boolean,
    val morningReportTime: LocalTime,
    val todakiEnabled: Boolean,
    val luckyActionReminderEnabled: Boolean,
)
