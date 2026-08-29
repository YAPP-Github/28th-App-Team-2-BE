package com.yapp.todakun.web.security

import com.yapp.todakun.web.security.annotation.BearerToken
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class BearerTokenArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(BearerToken::class.java) && parameter.parameterType == String::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): String = resolveToken(webRequest)

    private fun resolveToken(webRequest: NativeWebRequest): String {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
        val header = request?.getHeader(HttpHeaders.AUTHORIZATION)

        // authenticated() 경로에서만 사용되므로, Spring Security가 이미 인증을 완료해 헤더 형식을 보장한다.
        return extractBearerToken(header)!!
    }
}
