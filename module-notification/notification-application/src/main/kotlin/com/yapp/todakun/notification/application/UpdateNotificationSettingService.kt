package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.inbound.UpdateNotificationSettingCommand
import com.yapp.todakun.notification.port.inbound.UpdateNotificationSettingUseCase
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import kotlin.uuid.ExperimentalUuidApi

@CommandService
class UpdateNotificationSettingService(
    private val notificationSettingRepository: NotificationSettingRepository,
) : UpdateNotificationSettingUseCase {
    @ExperimentalUuidApi
    override fun update(command: UpdateNotificationSettingCommand): NotificationSetting {
        val setting =
            notificationSettingRepository.findByMemberId(command.memberId)
                ?: NotificationSetting.createDefault(command.memberId)
        val updated =
            setting.update(
                morningReportEnabled = command.morningReportEnabled,
                morningReportTime = command.morningReportTime,
                todakiEnabled = command.todakiEnabled,
                luckyActionReminderEnabled = command.luckyActionReminderEnabled,
            )
        return notificationSettingRepository.save(updated)
    }
}
