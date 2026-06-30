package com.yapp.todakun.web.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.yapp.todakun.common.code.ResponseCode
import com.yapp.todakun.web.code.CommonSuccessCode
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

/**
 * 모든 응답을 감싸는 공통 엔벨로프. 성공·실패가 동일한 형태를 가진다.
 * [data]는 값이 없으면 직렬화에서 생략된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommonResponse<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val data: T? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        /** 일반 성공(200). */
        fun <T> success(data: T): ResponseEntity<CommonResponse<T>> = ok(CommonSuccessCode.SUCCESS, data)

        /** 조회 성공(200). */
        fun <T> retrieved(data: T): ResponseEntity<CommonResponse<T>> = ok(CommonSuccessCode.RETRIEVED, data)

        /** 생성 성공(201). */
        fun <T> created(data: T): ResponseEntity<CommonResponse<T>> = ok(CommonSuccessCode.CREATED, data)

        /** 수정 성공(200, 본문 없음). */
        fun updated(): ResponseEntity<CommonResponse<Unit>> = ok(CommonSuccessCode.UPDATED, null)

        /** 삭제 성공(200, 본문 없음). */
        fun deleted(): ResponseEntity<CommonResponse<Unit>> = ok(CommonSuccessCode.DELETED, null)

        /** 에러 응답. [GlobalExceptionHandler]가 사용한다. */
        fun error(errorCode: ResponseCode): ResponseEntity<CommonResponse<Unit>> =
            ResponseEntity
                .status(errorCode.status)
                .body(CommonResponse(success = false, code = errorCode.code, message = errorCode.message))

        private fun <T> ok(
            code: ResponseCode,
            data: T?,
        ): ResponseEntity<CommonResponse<T>> =
            ResponseEntity
                .status(code.status)
                .body(CommonResponse(success = true, code = code.code, message = code.message, data = data))
    }
}
