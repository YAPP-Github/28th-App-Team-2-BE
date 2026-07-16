package com.yapp.todakun.shared

import java.util.UUID

interface GetMemberPort {
    fun findIdByOauth(
        provider: OauthProvider,
        providerId: String,
    ): UUID?
}
