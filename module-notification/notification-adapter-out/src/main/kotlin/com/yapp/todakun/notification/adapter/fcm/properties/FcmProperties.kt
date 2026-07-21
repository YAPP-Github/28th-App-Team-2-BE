package com.yapp.todakun.notification.adapter.fcm.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fcm")
data class FcmProperties(
    val enabled: Boolean = false,
    // Firebase 프로젝트 = GCP 프로젝트(GCP_PROJECT_ID 재사용). enabled=false면 사용하지 않으므로 nullable.
    val projectId: String? = null,
)
