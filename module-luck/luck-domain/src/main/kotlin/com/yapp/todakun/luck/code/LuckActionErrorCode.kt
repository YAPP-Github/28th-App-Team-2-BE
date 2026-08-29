package com.yapp.todakun.luck.code

import com.yapp.todakun.common.code.ResponseCode

enum class LuckActionErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    LUCK_ACTION_NOT_FOUND("LUCK-404", "존재하지 않는 행운 액션입니다.", 404),
    LUCK_ACTION_TITLE_TOO_LONG("LUCK-400", "제목은 최대 30자까지 입력할 수 있습니다.", 400),
    LUCK_ACTION_CONTENT_TOO_LONG("LUCK-400", "내용은 최대 200자까지 입력할 수 있습니다.", 400),
}
