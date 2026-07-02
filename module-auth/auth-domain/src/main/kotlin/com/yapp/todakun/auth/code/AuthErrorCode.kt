package com.yapp.todakun.auth.code

import com.yapp.todakun.common.code.ResponseCode

enum class AuthErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    TOKEN_EXPIRED("AUTH-401", "만료된 토큰입니다.", 401),
    TOKEN_INVALID("AUTH-401", "유효하지 않은 토큰입니다.", 401),
    TOKEN_BLACKLISTED("AUTH-401", "로그아웃된 토큰입니다.", 401),
}
