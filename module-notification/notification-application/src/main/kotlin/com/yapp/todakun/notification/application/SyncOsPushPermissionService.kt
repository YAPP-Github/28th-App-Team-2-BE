package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.inbound.SyncOsPushPermissionUseCase
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@CommandService
class SyncOsPushPermissionService(
    private val notificationSettingRepository: NotificationSettingRepository,
) : SyncOsPushPermissionUseCase {
    @ExperimentalUuidApi
    override fun sync(
        memberId: UUID,
        granted: Boolean,
    ): NotificationSetting {
        val setting =
            notificationSettingRepository.findByMemberId(memberId)
                ?: NotificationSetting.createDefault(memberId)
        return notificationSettingRepository.save(setting.syncOsPushPermission(granted))
    }
}
