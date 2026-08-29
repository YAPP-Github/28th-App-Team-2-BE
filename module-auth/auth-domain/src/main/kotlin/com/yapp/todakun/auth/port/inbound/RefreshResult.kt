package com.yapp.todakun.auth.port.inbound

import com.yapp.todakun.auth.token.IssuedAccessToken
import com.yapp.todakun.auth.token.IssuedRefreshToken

data class RefreshResult(
    val accessToken: IssuedAccessToken,
    val refreshToken: IssuedRefreshToken,
)
