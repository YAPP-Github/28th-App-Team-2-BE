package com.yapp.todakun.luck

import com.yapp.todakun.luck.exception.LuckActionContentTooLongException
import com.yapp.todakun.luck.exception.LuckActionTitleTooLongException

/** 행운 액션 생성 전 검증해야 할 도메인 불변식. [LuckAction.create] 호출 전에 검증한다. */
object LuckActionPolicy {
    private const val TITLE_MAX_LENGTH = 30
    private const val CONTENT_MAX_LENGTH = 200

    fun validate(
        title: String,
        content: String,
    ) {
        if (title.length > TITLE_MAX_LENGTH) {
            throw LuckActionTitleTooLongException()
        }
        if (content.length > CONTENT_MAX_LENGTH) {
            throw LuckActionContentTooLongException()
        }
    }
}
