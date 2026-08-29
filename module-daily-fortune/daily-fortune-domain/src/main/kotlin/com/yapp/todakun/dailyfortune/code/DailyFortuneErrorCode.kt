package com.yapp.todakun.dailyfortune.code

import com.yapp.todakun.common.code.ResponseCode

enum class DailyFortuneErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    DAILY_FORTUNE_NOT_FOUND("DAILY-FORTUNE-404", "존재하지 않는 오늘의 운세입니다.", 404),
    DAILY_FORTUNE_TITLE_TOO_LONG("DAILY-FORTUNE-400", "제목은 최대 30자까지 입력할 수 있습니다.", 400),
    DAILY_FORTUNE_CONTENT_TOO_LONG("DAILY-FORTUNE-400", "내용은 최대 800자까지 입력할 수 있습니다.", 400),
    DAILY_FORTUNE_ITEM_COUNT_MISMATCH("DAILY-FORTUNE-400", "행운 아이템과 주의 아이템은 각각 5개여야 합니다.", 400),
    DAILY_FORTUNE_HISTORY_TO_OUT_OF_RANGE("DAILY-FORTUNE-400", "조회 가능한 기간이 아닙니다.", 400),
    DAILY_FORTUNE_GENERATION_FAILED("DAILY-FORTUNE-500", "오늘의 운세 생성에 실패했습니다.", 500),
    DAILY_FORTUNE_EMPTY_RESPONSE("DAILY-FORTUNE-500", "AI로부터 빈 응답을 받았습니다.", 500),
    DAILY_FORTUNE_BATCH_ALREADY_RUNNING("DAILY-FORTUNE-409", "이미 실행 중인 오늘의 운세 배치가 있습니다.", 409),
    DAILY_FORTUNE_GENERATION_IN_PROGRESS("DAILY-FORTUNE-409", "오늘의 운세를 생성하는 중입니다. 잠시 후 다시 시도해 주세요.", 409),
    DAILY_FORTUNE_CIRCUIT_OPEN("DAILY-FORTUNE-503", "지금은 요청이 많아 오늘의 운세를 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.", 503),
    DAILY_FORTUNE_TIMEOUT("DAILY-FORTUNE-504", "오늘의 운세 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.", 504),
}
