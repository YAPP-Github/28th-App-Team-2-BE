package com.yapp.todakun.luck.policy

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionContentTooLongException
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.exception.LuckActionTitleTooLongException
import java.util.UUID

private const val TITLE_MAX_LENGTH = 30
private const val CONTENT_MAX_LENGTH = 200

/** 행운 액션 생성 전 검증해야 할 도메인 불변식. [com.yapp.todakun.luck.LuckAction.Companion.create] 호출 전에 검증한다. */
object LuckActionPolicy {
    fun validateLength(
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

    /** 다른 회원의 행운 액션 존재 여부가 드러나지 않도록 소유자가 다르면 동일하게 404로 처리한다. */
    fun validateOwner(
        luckAction: LuckAction,
        memberId: UUID,
    ) {
        if (luckAction.memberId != memberId) {
            throw LuckActionNotFoundException()
        }
    }
}
