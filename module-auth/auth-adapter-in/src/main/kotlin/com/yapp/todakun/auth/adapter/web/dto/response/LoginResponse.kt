package com.yapp.todakun.auth.adapter.web.dto.response

import com.yapp.todakun.auth.port.inbound.LoginResult

data class LoginResponse(
    val isNewMember: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val onboardingToken: String? = null,
) {
    companion object {
        fun from(result: LoginResult) =
            LoginResponse(
                isNewMember = result.isNewMember,
                accessToken = result.accessToken?.value,
                refreshToken = result.refreshToken?.value,
                onboardingToken = result.onboardingToken?.value,
            )
    }
}
