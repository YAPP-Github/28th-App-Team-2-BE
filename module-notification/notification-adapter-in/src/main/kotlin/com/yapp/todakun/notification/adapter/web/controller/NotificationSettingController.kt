package com.yapp.todakun.notification.adapter.web.controller

import com.yapp.todakun.notification.adapter.web.NotificationSettingApi
import com.yapp.todakun.notification.adapter.web.dto.request.UpdateNotificationSettingRequest
import com.yapp.todakun.notification.adapter.web.dto.response.NotificationSettingResponse
import com.yapp.todakun.notification.port.inbound.GetNotificationSettingUseCase
import com.yapp.todakun.notification.port.inbound.UpdateNotificationSettingUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class NotificationSettingController(
    private val getNotificationSettingUseCase: GetNotificationSettingUseCase,
    private val updateNotificationSettingUseCase: UpdateNotificationSettingUseCase,
) : NotificationSettingApi {
    override fun getSetting(memberId: UUID): ResponseEntity<CommonResponse<NotificationSettingResponse>> =
        CommonResponse.retrieved(NotificationSettingResponse.from(getNotificationSettingUseCase.getSetting(memberId)))

    override fun updateSetting(
        memberId: UUID,
        request: UpdateNotificationSettingRequest,
    ): ResponseEntity<CommonResponse<NotificationSettingResponse>> =
        CommonResponse.success(
            NotificationSettingResponse.from(updateNotificationSettingUseCase.update(request.toCommand(memberId))),
        )
}
