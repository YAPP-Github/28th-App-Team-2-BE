package com.yapp.todakun.saju.code

import com.yapp.todakun.common.code.ResponseCode

enum class SajuErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    SAJU_CHART_NOT_FOUND("SAJU-404", "사주 명식을 찾을 수 없습니다.", 404),
    SAJU_INPUT_INVALID("SAJU-400", "유효하지 않은 사주 입력값입니다.", 400),
    SAJU_YEAR_OUT_OF_RANGE("SAJU-422", "지원 범위(1900~2050년) 밖의 출생연도입니다.", 422),
    SAJU_PARTNER_LIMIT_EXCEEDED("SAJU-409", "상대방 사주는 최대 10명까지 등록할 수 있습니다.", 409),
    SAJU_CALCULATION_FAILED("SAJU-500", "사주 명식 계산 중 오류가 발생했습니다.", 500),
}
