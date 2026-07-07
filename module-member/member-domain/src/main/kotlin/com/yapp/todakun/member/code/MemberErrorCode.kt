package com.yapp.todakun.member.code

import com.yapp.todakun.common.code.ResponseCode

enum class MemberErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    MEMBER_NOT_FOUND("MEMBER-404", "회원을 찾을 수 없습니다.", 404),
}
