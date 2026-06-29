package com.yapp.todakun.web.code

import com.yapp.todakun.common.code.ResponseCode

/** 도메인 비종속 공통 에러 코드. 프레임워크 예외 매핑에 사용한다. */
enum class CommonErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    VALIDATION_ERROR("COMMON-400", "요청 값이 올바르지 않습니다", 400),
    MISSING_PARAMETER("COMMON-400", "필수 요청 파라미터가 누락되었습니다", 400),
    TYPE_MISMATCH("COMMON-400", "요청 파라미터 타입이 올바르지 않습니다", 400),
    INTERNAL_ERROR("COMMON-500", "서버 내부 오류가 발생했습니다", 500),
}
