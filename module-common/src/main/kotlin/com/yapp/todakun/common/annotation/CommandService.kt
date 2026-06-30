package com.yapp.todakun.common.annotation

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 변경(생성/수정/삭제) 유스케이스 서비스 스테레오타입.
 * `@Service` + `@Transactional`을 합성한다. `*-application` 모듈에서만 사용한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Service
@Transactional
annotation class CommandService
