package com.yapp.todakun.notification.fixture

import com.yapp.todakun.notification.NotificationSetting
import java.time.LocalTime
import java.util.UUID

private val SETTING_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")

object NotificationSettingFixture {
    fun create(
        id: UUID = SETTING_ID,
        memberId: UUID = MEMBER_ID,
        morningReportEnabled: Boolean = true,
        morningReportTime: LocalTime = LocalTime.of(8, 0),
        todakiEnabled: Boolean = false,
        luckyActionReminderEnabled: Boolean = true,
        osPushPermission: Boolean? = null,
    ): NotificationSetting =
        NotificationSetting.reconstitute(
            id,
            memberId,
            morningReportEnabled = morningReportEnabled,
            morningReportTime = morningReportTime,
            todakiEnabled = todakiEnabled,
            luckyActionReminderEnabled = luckyActionReminderEnabled,
            osPushPermission = osPushPermission,
        )
}
