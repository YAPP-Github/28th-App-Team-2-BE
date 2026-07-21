package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.notification.DeviceToken
import com.yapp.todakun.notification.port.inbound.RegisterDeviceTokenCommand
import com.yapp.todakun.notification.port.inbound.RegisterDeviceTokenUseCase
import com.yapp.todakun.notification.port.inbound.UnregisterDeviceTokenUseCase
import com.yapp.todakun.notification.port.outbound.DeviceTokenRepository
import kotlin.uuid.ExperimentalUuidApi

@CommandService
class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository,
) : RegisterDeviceTokenUseCase,
    UnregisterDeviceTokenUseCase {
    @ExperimentalUuidApi
    override fun register(command: RegisterDeviceTokenCommand) {
        // 같은 토큰이 이미 있으면(재로그인/기기 재배정) 소유 회원만 갱신하고, 없으면 새로 등록한다.
        val deviceToken =
            deviceTokenRepository.findByToken(command.token)?.reassign(command.memberId, command.platform)
                ?: DeviceToken.create(command.memberId, command.token, command.platform)
        deviceTokenRepository.save(deviceToken)
    }

    override fun unregister(token: String) = deviceTokenRepository.deleteByToken(token)
}
