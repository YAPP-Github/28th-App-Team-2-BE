package com.yapp.todakun.config

import com.yapp.todakun.auth.adapter.security.CustomAccessDeniedHandler
import com.yapp.todakun.auth.adapter.security.CustomAuthenticationEntryPoint
import com.yapp.todakun.auth.adapter.security.JwtAuthenticationFilter
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher
import org.springframework.web.cors.CorsConfigurationSource
import tools.jackson.databind.ObjectMapper

/** JWT 기반 무상태 인증 설정. 세션·CSRF는 사용하지 않는다. */
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun jwtAuthenticationFilter(
        accessTokenPort: AccessTokenPort,
        blacklistTokenPort: BlacklistTokenPort,
    ): JwtAuthenticationFilter = JwtAuthenticationFilter(accessTokenPort, blacklistTokenPort)

    @Bean
    fun customAuthenticationEntryPoint(objectMapper: ObjectMapper): CustomAuthenticationEntryPoint =
        CustomAuthenticationEntryPoint(objectMapper)

    @Bean
    fun customAccessDeniedHandler(objectMapper: ObjectMapper): CustomAccessDeniedHandler = CustomAccessDeniedHandler(objectMapper)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
        customAccessDeniedHandler: CustomAccessDeniedHandler,
        corsConfigurationSource: CorsConfigurationSource,
    ): SecurityFilterChain {
        http {
            cors { configurationSource = corsConfigurationSource }
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                // 원 요청(REQUEST 디스패치)이 아닌 컨테이너 내부 재디스패치는 인가에서 제외한다.
                //   ASYNC: SSE(SseEmitter) 같은 비동기 응답이 끝나면 컨테이너가 같은 요청을 필터 체인에
                //     한 번 더 태운다. 이때 JwtAuthenticationFilter(OncePerRequestFilter)는 기본적으로 async
                //     디스패치를 건너뛰고 SecurityContextHolderFilter도 이미 적용된 요청이라 복원하지 않아
                //     SecurityContext가 비어 있다 → anyRequest.authenticated에 걸려 AuthorizationDeniedException.
                //     응답은 이미 커밋된 뒤라 에러 본문을 쓸 수 없어 Tomcat이 커넥션을 그대로 끊고,
                //     클라이언트에는 스트림 비정상 종료로 보인다(HTTP/2 INTERNAL_ERROR, HTTP/1.1 incomplete chunked).
                //   ERROR: /error 포워딩도 같은 이유로 거부돼 의도한 에러 응답 대신 빈 응답이 나간다.
                // 둘 다 이미 인가를 통과한 원 요청의 연속이므로 permitAll이 안전하다. 외부에서 /error를
                // 직접 호출하는 건 REQUEST 디스패치라 아래 anyRequest.authenticated가 그대로 막는다.
                authorize(DispatcherTypeRequestMatcher(DispatcherType.ASYNC), permitAll)
                authorize(DispatcherTypeRequestMatcher(DispatcherType.ERROR), permitAll)
                SecurityPaths.SWAGGER.forEach { pattern -> authorize(pattern, permitAll) }
                SecurityPaths.ACTUATOR.forEach { pattern -> authorize(pattern, permitAll) }
                SecurityPaths.PUBLIC.forEach { pattern -> authorize(pattern, permitAll) }
                authorize(anyRequest, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = customAuthenticationEntryPoint
                accessDeniedHandler = customAccessDeniedHandler
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }

        return http.build()
    }
}
