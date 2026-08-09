package com.yapp.todakun.notification.adapter.web.controller

import com.yapp.todakun.notification.adapter.web.AdminNoticeApi
import com.yapp.todakun.notification.adapter.web.dto.request.PublishNoticeRequest
import com.yapp.todakun.notification.port.inbound.PublishNoticeUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminNoticeController(
    private val publishNoticeUseCase: PublishNoticeUseCase,
) : AdminNoticeApi {
    override fun publishNotice(request: PublishNoticeRequest): ResponseEntity<CommonResponse<Unit>> {
        publishNoticeUseCase.publish(request.title, request.content, request.deepLink)
        return CommonResponse.created(Unit)
    }
}
