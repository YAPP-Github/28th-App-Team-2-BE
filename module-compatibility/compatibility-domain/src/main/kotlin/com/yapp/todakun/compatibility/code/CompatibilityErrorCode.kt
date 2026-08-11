package com.yapp.todakun.compatibility.code

import com.yapp.todakun.common.code.ResponseCode

enum class CompatibilityErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    COMPATIBILITY_NOT_FOUND("COMPATIBILITY-404", "존재하지 않는 궁합입니다.", 404),
    COMPATIBILITY_SCORE_OUT_OF_RANGE("COMPATIBILITY-400", "궁합 점수는 0점에서 100점 사이여야 합니다.", 400),
    COMPATIBILITY_HEADLINE_TOO_LONG("COMPATIBILITY-400", "헤드라인은 최대 50자까지 입력할 수 있습니다.", 400),
    COMPATIBILITY_SUBHEADLINE_TOO_LONG("COMPATIBILITY-400", "서브헤드라인은 최대 100자까지 입력할 수 있습니다.", 400),
    COMPATIBILITY_SUMMARY_TOO_LONG("COMPATIBILITY-400", "요약은 최대 200자까지 입력할 수 있습니다.", 400),
    COMPATIBILITY_OHAENG_ELEMENT_MISMATCH("COMPATIBILITY-400", "오행 비율은 5개 오행이 모두 있어야 합니다.", 400),
    COMPATIBILITY_RELATIONSHIP_TYPE_INVALID("COMPATIBILITY-400", "올바른 관계 유형이 아닙니다.", 400),
    COMPATIBILITY_OHAENG_EMPTY("COMPATIBILITY-500", "오행 데이터가 비어 궁합 오행 비율을 계산할 수 없습니다.", 500),
    COMPATIBILITY_OHAENG_COUNT_NEGATIVE("COMPATIBILITY-500", "오행 글자 수는 음수일 수 없습니다.", 500),
    COMPATIBILITY_GENERATION_FAILED("COMPATIBILITY-500", "궁합 분석 생성에 실패했습니다.", 500),
    COMPATIBILITY_EMPTY_RESPONSE("COMPATIBILITY-500", "AI로부터 빈 응답을 받았습니다.", 500),
}
