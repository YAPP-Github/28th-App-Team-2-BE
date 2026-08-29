package com.yapp.todakun.dayfortune.code

import com.yapp.todakun.common.code.ResponseCode

enum class DaySelectionFortuneErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    DAY_SELECTION_FORTUNE_NOT_FOUND("FORTUNE-404", "존재하지 않는 택일 운세입니다.", 404),
    DAY_SELECTION_FORTUNE_TITLE_TOO_LONG("FORTUNE-500", "제목은 최대 30자까지 입력할 수 있습니다.", 500),
    DAY_SELECTION_FORTUNE_CONTENT_TOO_LONG("FORTUNE-500", "내용은 최대 500자까지 입력할 수 있습니다.", 500),
    DAY_SELECTION_FORTUNE_CATEGORY_COUNT_MISMATCH("FORTUNE-500", "행운 카테고리는 3개여야 합니다.", 500),
    DAY_SELECTION_FORTUNE_CATEGORY_DUPLICATED("FORTUNE-500", "행운 카테고리는 중복 선택할 수 없습니다.", 500),
    DAY_SELECTION_FORTUNE_STAR_OUT_OF_RANGE("FORTUNE-500", "별점은 1점에서 3점 사이여야 합니다.", 500),
    DAY_SELECTION_FORTUNE_SCORE_OUT_OF_RANGE("FORTUNE-500", "점수는 0점에서 100점 사이여야 합니다.", 500),
    DAY_SELECTION_FORTUNE_DATE_IN_PAST("FORTUNE-400", "과거 날짜는 선택할 수 없습니다.", 400),
    DAY_SELECTION_FORTUNE_INVALID_PURPOSE("FORTUNE-400", "올바른 택일 목적 값이 아닙니다.", 400),
    DAY_SELECTION_FORTUNE_GENERATION_FAILED("FORTUNE-500", "택일 운세 생성에 실패했습니다.", 500),
    DAY_SELECTION_FORTUNE_EMPTY_RESPONSE("FORTUNE-500", "AI로부터 빈 응답을 받았습니다.", 500),
    DAY_SELECTION_FORTUNE_CIRCUIT_OPEN("FORTUNE-503", "지금은 요청이 많아 택일 운세를 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.", 503),
    DAY_SELECTION_FORTUNE_TIMEOUT("FORTUNE-504", "택일 운세 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.", 504),
}
