package com.yapp.todakun.web.security

/**
 * 인증 필터가 액세스 토큰을 파싱하며 얻은 값 중, 토큰 폐기(로그아웃/탈퇴)에 필요한 jti·남은 만료 시간을
 * 다른 도메인 컨트롤러와 공유하기 위한 서블릿 요청 속성 키. 원본 토큰 문자열을 다시 파싱하지 않도록,
 * 이미 검증된 값만 원시 타입으로 노출한다.
 */
object AccessTokenAttributes {
    const val JTI = "ACCESS_TOKEN_JTI"
    const val REMAINING_SECONDS = "ACCESS_TOKEN_REMAINING_SECONDS"
}
