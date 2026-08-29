package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.NotificationSetting

interface UpdateNotificationSettingUseCase {
    fun update(command: UpdateNotificationSettingCommand): NotificationSetting
}
