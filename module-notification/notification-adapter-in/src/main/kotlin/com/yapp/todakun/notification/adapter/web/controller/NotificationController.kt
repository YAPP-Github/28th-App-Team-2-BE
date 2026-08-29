package com.yapp.todakun.notification.adapter.web.controller

import com.yapp.todakun.notification.adapter.web.NotificationApi
import com.yapp.todakun.notification.adapter.web.dto.response.NotificationListResponse
import com.yapp.todakun.notification.port.inbound.GetNotificationsUseCase
import com.yapp.todakun.notification.port.inbound.ReadNotificationUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class NotificationController(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val readNotificationUseCase: ReadNotificationUseCase,
) : NotificationApi {
    override fun getNotifications(memberId: UUID): ResponseEntity<CommonResponse<NotificationListResponse>> =
        CommonResponse.retrieved(NotificationListResponse.from(getNotificationsUseCase.getNotifications(memberId)))

    override fun readNotification(
        memberId: UUID,
        notificationId: UUID,
    ): ResponseEntity<CommonResponse<Unit>> {
        readNotificationUseCase.read(memberId, notificationId)
        return CommonResponse.updated()
    }

    override fun readAllNotifications(memberId: UUID): ResponseEntity<CommonResponse<Unit>> {
        readNotificationUseCase.readAll(memberId)
        return CommonResponse.updated()
    }
}
