package com.yapp.todakun.common.code

/**
 * 성공·에러 응답 코드의 공통 계약. [status]는 HTTP 상태 코드를 운반한다.
 */
interface ResponseCode {
    val code: String
    val message: String
    val status: Int
}
