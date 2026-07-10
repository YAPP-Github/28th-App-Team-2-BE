package com.yapp.todakun.auth.port.outbound

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.shared.OauthProvider

interface OauthPort {
    fun fetchProfile(
        provider: OauthProvider,
        token: String,
    ): OauthMemberProfile
}
