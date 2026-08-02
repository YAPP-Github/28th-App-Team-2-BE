package com.yapp.todakun.auth.adapter.oauth.apple

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.apple")
data class AppleOauthProperties(
    val clientIds: List<String>,
    val jwkSetUri: String,
    val issuer: String,
    val teamId: String,
    val keyId: String,
    // .p8 파일의 PEM 헤더/개행을 제거한 PKCS8 DER base64 문자열.
    val privateKey: String,
    val tokenEndpoint: String,
    val revokeEndpoint: String,
)
