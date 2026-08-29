package com.yapp.todakun.config

import com.yapp.todakun.web.openapi.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger(OpenAPI) 문서 설정.
 *
 * - JWT Bearer 인증 스킴을 전역 보안 요구사항으로 적용한다(모든 API에 자물쇠 표시).
 * - 인증이 필요 없는 API는 `*Api` 인터페이스 메서드에 [DisableSwaggerSecurity]를 붙이면
 *   [disableSecurityCustomizer]가 해당 메서드의 보안 요구사항(자물쇠)을 문서에서 제거한다.
 */

private const val SECURITY_SCHEME_NAME = "JWT "

@Configuration
class SwaggerConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("todakun API")
                    .description("YAPP 28기 App 2팀 토닥운 백엔드 API 문서")
                    .version("0.0.1"),
            ).addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(
                Components().addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )

    /**
     * [DisableSwaggerSecurity]가 붙은 핸들러 메서드는 보안 요구사항을 비워(`emptyList`)
     * Swagger 문서에서 자물쇠를 제거한다.
     */
    @Bean
    fun disableSecurityCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            if (handlerMethod.hasMethodAnnotation(DisableSwaggerSecurity::class.java)) {
                operation.security(emptyList())
            }
            operation
        }
}
