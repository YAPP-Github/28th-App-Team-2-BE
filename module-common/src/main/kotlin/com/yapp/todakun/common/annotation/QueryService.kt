package com.yapp.todakun.common.annotation

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 조회 유스케이스 서비스 스테레오타입.
 * `@Service` + `@Transactional(readOnly = true)`를 합성한다. `*-application` 모듈에서만 사용한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Service
@Transactional(readOnly = true)
annotation class QueryService
