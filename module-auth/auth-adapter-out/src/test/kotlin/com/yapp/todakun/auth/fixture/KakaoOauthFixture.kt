package com.yapp.todakun.auth.fixture

import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoAccount
import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoMemberInfoResponse
import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoOauthProperties

private const val USER_ME_URI = "https://kapi.kakao.com/v2/user/me"

object KakaoOauthFixture {
    const val PROVIDER_ID = 123456L
    const val EMAIL = "test@todakun.com"

    fun properties(userMeUri: String = USER_ME_URI): KakaoOauthProperties = KakaoOauthProperties(userMeUri = userMeUri)

    fun memberInfoResponse(
        id: Long = PROVIDER_ID,
        email: String? = EMAIL,
        isEmailVerified: Boolean = true,
    ): KakaoMemberInfoResponse =
        KakaoMemberInfoResponse(
            id = id,
            kakaoAccount = KakaoAccount(email = email, isEmailVerified = isEmailVerified),
        )
}
