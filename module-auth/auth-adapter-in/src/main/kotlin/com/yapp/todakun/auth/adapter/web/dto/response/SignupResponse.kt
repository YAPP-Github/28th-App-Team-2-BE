package com.yapp.todakun.auth.adapter.web.dto.response

import com.yapp.todakun.auth.port.inbound.SignupResult

data class SignupResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun from(result: SignupResult) =
            SignupResponse(
                accessToken = result.accessToken.value,
                refreshToken = result.refreshToken.value,
            )
    }
}
