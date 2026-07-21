package com.yapp.todakun.luck.code

import com.yapp.todakun.common.code.ResponseCode

enum class LuckActionErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    LUCK_ACTION_NOT_FOUND("LUCK-404", "존재하지 않는 행운 액션입니다.", 404),
}
