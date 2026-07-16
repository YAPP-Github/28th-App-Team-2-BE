package com.yapp.todakun.auth.adapter.security

import com.yapp.todakun.auth.code.AuthErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import tools.jackson.databind.ObjectMapper

/**
 * 인증되지 않은 요청이 보호된 리소스에 접근할 때 호출된다.
 * [JwtAuthenticationFilter]가 [AuthenticationFailureHolder]에 남긴 실패 원인이 있으면 그 코드를,
 * 없으면(토큰이 아예 없는 경우) 기본 401 코드로 응답한다.
 */
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val errorCode = AuthenticationFailureHolder.get(request)?.errorCode ?: AuthErrorCode.AUTHENTICATION_REQUIRED
        response.writeSecurityError(objectMapper, errorCode)
    }
}
