package com.yapp.todakun.config

import com.yapp.todakun.auth.adapter.security.CustomAccessDeniedHandler
import com.yapp.todakun.auth.adapter.security.CustomAuthenticationEntryPoint
import com.yapp.todakun.auth.adapter.security.JwtAuthenticationFilter
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.BlacklistTokenPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
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
