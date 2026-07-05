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
    AUTHENTICATION_REQUIRED("AUTH-401", "인증이 필요합니다.", 401),
    ACCESS_DENIED("AUTH-403", "접근 권한이 없습니다.", 403),
    OAUTH_TOKEN_INVALID("AUTH-401", "유효하지 않은 소셜 로그인 토큰입니다.", 401),
    OAUTH_EMAIL_REQUIRED("AUTH-401", "이메일 제공에 동의해야 로그인할 수 있습니다.", 401),
}
