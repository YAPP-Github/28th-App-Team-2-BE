package com.yapp.todakun.auth.adapter.security

import com.yapp.todakun.auth.claims.AccessTokenClaims
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.common.exception.UnauthorizedException
import com.yapp.todakun.web.security.extractBearerToken
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * `Authorization: Bearer {accessToken}` 헤더를 검증해 [SecurityContextHolder]에 인증 정보를 채워 넣는다.
 * 토큰이 없거나 검증에 실패해도 체인은 그대로 진행시키고, 실패 원인만 [AuthenticationFailureHolder]에 남긴다.
 * 실제 인가 거부와 실패 응답 작성은 [CustomAuthenticationEntryPoint]에 위임한다(인증 로직과 응답 포맷팅 책임 분리).
 */
class JwtAuthenticationFilter(
    private val accessTokenPort: AccessTokenPort,
    private val blacklistTokenPort: BlacklistTokenPort,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        resolveToken(request)?.let {
            try {
                authenticate(parseClaims(it))
            } catch (e: BusinessException) {
                SecurityContextHolder.clearContext()
                AuthenticationFailureHolder.set(request, e)
            } catch (e: Exception) {
                SecurityContextHolder.clearContext()
                AuthenticationFailureHolder.set(request, UnauthorizedException(AuthErrorCode.TOKEN_INVALID))
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION))

    private fun parseClaims(token: String): AccessTokenClaims {
        val claims = accessTokenPort.parse(token)

        if (blacklistTokenPort.isBlacklisted(claims.jti)) {
            throw UnauthorizedException(AuthErrorCode.TOKEN_BLACKLISTED)
        }

        return claims
    }

    private fun authenticate(claims: AccessTokenClaims) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(claims.memberId, null, emptyList())
    }
}
