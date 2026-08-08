package com.yapp.todakun.notification.adapter.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class SyncOsPushPermissionRequest(
    @field:Schema(description = "OS 알림 권한 허용 여부(클라이언트가 감지한 최신 값)", example = "true")
    val granted: Boolean,
)
