package com.yapp.todakun.auth.adapter.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.yapp.todakun.auth.AccessTokenClaims
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.port.AccessTokenPort
import com.yapp.todakun.auth.port.BlacklistTokenPort
import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.common.exception.UnauthorizedException
import com.yapp.todakun.web.response.CommonResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

private const val BEARER_PREFIX = "Bearer "

/**
 * `Authorization: Bearer {accessToken}` 헤더를 검증해 [SecurityContextHolder]에 인증 정보를 채워 넣는다.
 * 토큰이 없으면 그냥 통과시킨다(인가 여부는 SecurityConfig가 판단).
 */
class JwtAuthenticationFilter(
    private val accessTokenPort: AccessTokenPort,
    private val blacklistTokenPort: BlacklistTokenPort,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            authenticate(parseClaims(token))
            filterChain.doFilter(request, response)
        } catch (e: BusinessException) {
            SecurityContextHolder.clearContext()
            writeErrorResponse(response, e)
        }
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        return header.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)
    }

    private fun parseClaims(token: String): AccessTokenClaims {
        val claims = accessTokenPort.parse(token)

        if (blacklistTokenPort.isBlacklisted(claims.jti)) {
            throw UnauthorizedException(AuthErrorCode.TOKEN_BLACKLISTED)
        }

        return claims
    }

    private fun authenticate(claims: AccessTokenClaims) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(claims.memberId, null, emptyList())
    }

    private fun writeErrorResponse(
        response: HttpServletResponse,
        e: BusinessException,
    ) {
        response.status = e.errorCode.status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(objectMapper.writeValueAsString(CommonResponse.error(e.errorCode).body))
    }
}
