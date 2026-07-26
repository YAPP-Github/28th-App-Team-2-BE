package com.yapp.todakun.auth.adapter.web.dto.response

import com.yapp.todakun.auth.port.inbound.RefreshResult

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun from(result: RefreshResult) =
            RefreshResponse(
                accessToken = result.accessToken.value,
                refreshToken = result.refreshToken.value,
            )
    }
}
