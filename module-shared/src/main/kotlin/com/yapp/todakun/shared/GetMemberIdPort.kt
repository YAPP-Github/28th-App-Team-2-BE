package com.yapp.todakun.shared

import java.util.UUID

interface GetMemberIdPort {
    fun findIdByOauth(
        provider: OauthProvider,
        providerId: String,
    ): UUID?
}
