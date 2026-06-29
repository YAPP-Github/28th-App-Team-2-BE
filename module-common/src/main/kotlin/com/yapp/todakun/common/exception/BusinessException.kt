package com.yapp.todakun.common.exception

import com.yapp.todakun.common.code.ResponseCode

/**
 * 모든 비즈니스 예외의 베이스. [errorCode]가 응답 코드와 HTTP 상태를 운반한다.
 * 직접 [RuntimeException]을 던지지 말고 이 타입(또는 하위)을 사용한다.
 */
abstract class BusinessException(
    val errorCode: ResponseCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)
