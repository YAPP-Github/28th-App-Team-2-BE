package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.NotificationSetting
import java.util.UUID

interface GetNotificationSettingUseCase {
    fun getSetting(memberId: UUID): NotificationSetting
}
