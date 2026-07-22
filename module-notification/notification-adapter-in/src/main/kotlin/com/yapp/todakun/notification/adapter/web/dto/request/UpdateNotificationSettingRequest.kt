package com.yapp.todakun.notification.adapter.web.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.yapp.todakun.notification.port.inbound.UpdateNotificationSettingCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime
import java.util.UUID

data class UpdateNotificationSettingRequest(
    @field:Schema(description = "아침 운 리포트 수신 여부", example = "true")
    val morningReportEnabled: Boolean,
    @field:Schema(description = "아침 운 리포트 받을 시간(30분 단위)", example = "08:00", type = "string")
    @field:JsonFormat(pattern = "HH:mm")
    val morningReportTime: LocalTime,
    @field:Schema(description = "토닥이 답변 완료 알림 수신 여부", example = "true")
    val todakiEnabled: Boolean,
    @field:Schema(description = "행운 액션 리마인드 수신 여부", example = "true")
    val luckyActionReminderEnabled: Boolean,
) {
    fun toCommand(memberId: UUID) =
        UpdateNotificationSettingCommand(
            memberId = memberId,
            morningReportEnabled = morningReportEnabled,
            morningReportTime = morningReportTime,
            todakiEnabled = todakiEnabled,
            luckyActionReminderEnabled = luckyActionReminderEnabled,
        )
}
