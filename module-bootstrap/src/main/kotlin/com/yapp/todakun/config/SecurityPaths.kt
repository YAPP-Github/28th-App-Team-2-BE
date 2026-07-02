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
}
