package com.yapp.todakun.web.exception.handler

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.web.code.CommonErrorCode
import com.yapp.todakun.web.response.CommonResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 모든 예외를 공통 엔벨로프([CommonResponse])로 변환한다. common-web에 한 번만 두며
 * bootstrap의 컴포넌트 스캔으로 전역 등록된다. HTTP 상태는 [BusinessException.errorCode]가 결정한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<CommonResponse<Unit>> {
        if (e.errorCode.status >= 500) {
            log.error("BusinessException 발생: {}", e.errorCode.code, e)
        } else {
            log.warn("BusinessException 발생: {} - {}", e.errorCode.code, e.errorCode.message)
        }
        return CommonResponse.error(e.errorCode)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<CommonResponse<Unit>> {
        log.warn("Validation 오류: {}", e.message)
        return CommonResponse.error(CommonErrorCode.VALIDATION_ERROR)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(e: MissingServletRequestParameterException): ResponseEntity<CommonResponse<Unit>> {
        log.warn("요청 파라미터 누락: {}", e.message)
        return CommonResponse.error(CommonErrorCode.MISSING_PARAMETER)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<CommonResponse<Unit>> {
        log.warn("타입 불일치: {}", e.message)
        return CommonResponse.error(CommonErrorCode.TYPE_MISMATCH)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<CommonResponse<Unit>> {
        log.warn("요청 본문 파싱 실패: {}", e.message)
        return CommonResponse.error(CommonErrorCode.MALFORMED_REQUEST)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<CommonResponse<Unit>> {
        log.warn("리소스를 찾을 수 없음: {}", e.message)
        return CommonResponse.error(CommonErrorCode.NOT_FOUND)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<CommonResponse<Unit>> {
        log.error("Unexpected error 발생: {}", e.message, e)
        return CommonResponse.error(CommonErrorCode.INTERNAL_ERROR)
    }
}
