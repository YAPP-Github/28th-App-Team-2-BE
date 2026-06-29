package com.yapp.todakun.web.openapi.annotation

/**
 * 인증이 필요 없는 API를 표시한다. bootstrap의 springdoc `OperationCustomizer`가
 * 이 메서드의 시큐리티 요구사항(자물쇠)을 Swagger 문서에서 제거한다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisableSwaggerSecurity
