package com.yapp.todakun.notification.adapter.web.controller

import com.yapp.todakun.notification.adapter.web.DeviceTokenApi
import com.yapp.todakun.notification.adapter.web.dto.request.RegisterDeviceTokenRequest
import com.yapp.todakun.notification.adapter.web.dto.request.UnregisterDeviceTokenRequest
import com.yapp.todakun.notification.port.inbound.RegisterDeviceTokenUseCase
import com.yapp.todakun.notification.port.inbound.UnregisterDeviceTokenUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DeviceTokenController(
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
    private val unregisterDeviceTokenUseCase: UnregisterDeviceTokenUseCase,
) : DeviceTokenApi {
    override fun register(
        memberId: UUID,
        request: RegisterDeviceTokenRequest,
    ): ResponseEntity<CommonResponse<Unit>> {
        registerDeviceTokenUseCase.register(request.toCommand(memberId))
        return CommonResponse.success(Unit)
    }

    override fun unregister(
        memberId: UUID,
        request: UnregisterDeviceTokenRequest,
    ): ResponseEntity<CommonResponse<Unit>> {
        unregisterDeviceTokenUseCase.unregister(memberId, request.token)
        return CommonResponse.deleted()
    }
}
