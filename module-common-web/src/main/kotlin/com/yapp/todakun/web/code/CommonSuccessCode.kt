package com.yapp.todakun.web.code

import com.yapp.todakun.common.code.ResponseCode

/** 도메인 비종속 공통 성공 코드. HTTP 상태는 [status]가 운반한다. */
enum class CommonSuccessCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    SUCCESS("COMMON-200", "요청이 완료되었습니다", 200),
    RETRIEVED("COMMON-200", "조회가 완료되었습니다", 200),
    CREATED("COMMON-201", "생성이 완료되었습니다", 201),
    UPDATED("COMMON-200", "수정이 완료되었습니다", 200),
    DELETED("COMMON-200", "삭제가 완료되었습니다", 200),
}
