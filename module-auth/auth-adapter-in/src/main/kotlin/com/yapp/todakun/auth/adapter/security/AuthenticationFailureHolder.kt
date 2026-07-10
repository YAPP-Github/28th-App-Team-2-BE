package com.yapp.todakun.auth.adapter.security

import com.yapp.todakun.common.exception.BusinessException
import jakarta.servlet.http.HttpServletRequest

private const val KEY = "com.yapp.todakun.auth.exception"

/**
 * [JwtAuthenticationFilter]가 남긴 인증 실패 원인을 [CustomAuthenticationEntryPoint]에 전달하는 통로.
 * 요청 속성 키와 캐스팅을 이 안에 감춰서, 양쪽 클래스가 문자열 상수를 직접 다루지 않게 한다.
 */
internal object AuthenticationFailureHolder {
    fun set(
        request: HttpServletRequest,
        exception: BusinessException,
    ) {
        request.setAttribute(KEY, exception)
    }

    fun get(request: HttpServletRequest): BusinessException? = request.getAttribute(KEY) as? BusinessException
}
