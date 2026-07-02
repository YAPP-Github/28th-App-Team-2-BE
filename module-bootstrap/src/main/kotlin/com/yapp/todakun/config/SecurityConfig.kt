package com.yapp.todakun.config

import com.yapp.todakun.auth.adapter.web.CustomAccessDeniedHandler
import com.yapp.todakun.auth.adapter.web.CustomAuthenticationEntryPoint
import com.yapp.todakun.auth.adapter.web.JwtAuthenticationFilter
import com.yapp.todakun.auth.port.AccessTokenPort
import com.yapp.todakun.auth.port.BlacklistTokenPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.ObjectMapper

private val PERMIT_ALL_PATHS =
    arrayOf(
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
    )

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
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(*PERMIT_ALL_PATHS).permitAll()
                it.anyRequest().authenticated()
            }.exceptionHandling {
                it.authenticationEntryPoint(customAuthenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
