package com.yapp.todakun.yearfortune.code

import com.yapp.todakun.common.code.ResponseCode

enum class YearSelectionFortuneErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    YEAR_SELECTION_FORTUNE_NOT_FOUND("FORTUNE-404", "존재하지 않는 연도별 운세입니다.", 404),
    YEAR_SELECTION_FORTUNE_TITLE_TOO_LONG("FORTUNE-400", "제목은 최대 30자까지 입력할 수 있습니다.", 400),
    YEAR_SELECTION_FORTUNE_CONTENT_TOO_LONG("FORTUNE-400", "내용은 최대 500자까지 입력할 수 있습니다.", 400),
    YEAR_SELECTION_FORTUNE_CATEGORY_COUNT_MISMATCH("FORTUNE-400", "행운 카테고리는 3개여야 합니다.", 400),
    YEAR_SELECTION_FORTUNE_CATEGORY_DUPLICATED("FORTUNE-400", "행운 카테고리는 중복 선택할 수 없습니다.", 400),
    YEAR_SELECTION_FORTUNE_STAR_OUT_OF_RANGE("FORTUNE-400", "별점은 1점에서 3점 사이여야 합니다.", 400),
    YEAR_SELECTION_FORTUNE_YEAR_OUT_OF_RANGE("FORTUNE-400", "연도는 ±5년까지만 생성할 수 있습니다.", 400),
    YEAR_SELECTION_FORTUNE_SCORE_OUT_OF_RANGE("FORTUNE-400", "점수는 0점에서 100점 사이여야 합니다.", 400),
    YEAR_SELECTION_FORTUNE_GENERATION_FAILED("FORTUNE-500", "연도별 운세 생성에 실패했습니다.", 500),
    YEAR_SELECTION_FORTUNE_EMPTY_RESPONSE("FORTUNE-500", "AI로부터 빈 응답을 받았습니다.", 500),
}
