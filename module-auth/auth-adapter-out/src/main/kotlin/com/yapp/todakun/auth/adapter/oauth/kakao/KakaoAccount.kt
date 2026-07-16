package com.yapp.todakun.auth.adapter.oauth.kakao

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoAccount(
    val email: String?,
    @JsonProperty("is_email_verified") val isEmailVerified: Boolean = false,
)
