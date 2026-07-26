package com.yapp.todakun.config

/** [SecurityConfig]에서 사용하는 URL 경로 상수 모음. */
object SecurityPaths {
    /** Swagger 문서/툴링 경로. */
    val SWAGGER =
        arrayOf(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
        )

    /** 내부 헬스체크(Blue/Green) · 메트릭 스크래핑(Alloy) 경로. */
    val ACTUATOR =
        arrayOf(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/prometheus",
        )

    /** 인증 없이 접근 가능한 API 경로. */
    val PUBLIC =
        arrayOf(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            // access token 없이(만료된 상태로) 호출되므로 refresh token 자체로 검증한다.
            "/api/v1/auth/refresh",
            // 약관 목록은 온보딩 단계(가입 전)에서도 노출되어야 하므로 공개한다. 동의 저장(POST)은 인증 필요.
            "/api/v1/terms",
        )
}
