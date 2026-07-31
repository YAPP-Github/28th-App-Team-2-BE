package com.yapp.todakun.auth.adapter.oauth.apple

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AppleTokenResponse(
    @JsonProperty("refresh_token") val refreshToken: String,
)
