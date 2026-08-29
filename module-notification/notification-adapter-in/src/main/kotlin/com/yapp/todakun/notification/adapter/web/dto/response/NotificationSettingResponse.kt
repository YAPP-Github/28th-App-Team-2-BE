package com.yapp.todakun.notification.adapter.web.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.yapp.todakun.notification.NotificationSetting
import java.time.LocalTime

data class NotificationSettingResponse(
    val morningReportEnabled: Boolean,
    @field:JsonFormat(pattern = "HH:mm")
    val morningReportTime: LocalTime,
    val todakiEnabled: Boolean,
    val luckyActionReminderEnabled: Boolean,
    val osPushPermission: Boolean?,
) {
    companion object {
        fun from(setting: NotificationSetting) =
            NotificationSettingResponse(
                morningReportEnabled = setting.morningReportEnabled,
                morningReportTime = setting.morningReportTime,
                todakiEnabled = setting.todakiEnabled,
                luckyActionReminderEnabled = setting.luckyActionReminderEnabled,
                osPushPermission = setting.osPushPermission,
            )
    }
}
