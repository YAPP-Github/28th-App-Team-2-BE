package com.yapp.todakun.dailyfortune.code

import com.yapp.todakun.common.code.ResponseCode

enum class DailyFortuneErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    DAILY_FORTUNE_NOT_FOUND("FORTUNE-404", "존재하지 않는 오늘의 운세입니다.", 404),
    DAILY_FORTUNE_TITLE_TOO_LONG("FORTUNE-400", "제목은 최대 30자까지 입력할 수 있습니다.", 400),
    DAILY_FORTUNE_CONTENT_TOO_LONG("FORTUNE-400", "내용은 최대 800자까지 입력할 수 있습니다.", 400),
    DAILY_FORTUNE_ITEM_COUNT_MISMATCH("FORTUNE-400", "행운 아이템과 주의 아이템은 각각 5개여야 합니다.", 400),
    DAILY_FORTUNE_HISTORY_TO_OUT_OF_RANGE("FORTUNE-400", "조회 가능한 기간이 아닙니다.", 400),
}
