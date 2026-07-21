package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.inbound.GetNotificationSettingUseCase
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@QueryService
class GetNotificationSettingService(
    private val notificationSettingRepository: NotificationSettingRepository,
) : GetNotificationSettingUseCase {
    @ExperimentalUuidApi
    override fun getSetting(memberId: UUID): NotificationSetting =
        // 설정을 한 번도 저장하지 않은 회원은 저장 없이 기본값(전부 OFF)을 반환한다.
        notificationSettingRepository.findByMemberId(memberId) ?: NotificationSetting.createDefault(memberId)
}
