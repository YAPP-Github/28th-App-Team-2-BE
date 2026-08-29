package com.yapp.todakun.auth.adapter.oauth.kakao

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoMemberInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount,
)
