package com.yapp.todakun.auth.adapter.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.yapp.todakun.auth.code.AuthErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler

/** 인증은 됐지만 권한이 부족해 접근이 거부됐을 때 호출된다. */
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.writeSecurityError(objectMapper, AuthErrorCode.ACCESS_DENIED)
    }
}
