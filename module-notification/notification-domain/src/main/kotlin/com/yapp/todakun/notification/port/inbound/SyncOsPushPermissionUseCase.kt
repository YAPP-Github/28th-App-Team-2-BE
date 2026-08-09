package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.NotificationSetting
import java.util.UUID

interface SyncOsPushPermissionUseCase {
    fun sync(
        memberId: UUID,
        granted: Boolean,
    ): NotificationSetting
}
