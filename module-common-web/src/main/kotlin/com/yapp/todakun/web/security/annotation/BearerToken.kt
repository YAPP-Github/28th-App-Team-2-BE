package com.yapp.todakun.web.security.annotation

/**
 * `Authorization: Bearer {token}` 헤더에서 접두사를 제거한 원본 토큰 문자열을 주입한다.
 * 인증(authenticated)이 필요한 API에서만 사용해야 하며, 미인증 요청은 Spring Security가 컨트롤러 진입 전에 걸러낸다.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class BearerToken
