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
        )
}
