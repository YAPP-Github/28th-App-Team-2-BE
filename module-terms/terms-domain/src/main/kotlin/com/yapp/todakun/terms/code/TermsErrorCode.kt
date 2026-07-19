package com.yapp.todakun.terms.code

import com.yapp.todakun.common.code.ResponseCode

enum class TermsErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    TERMS_NOT_FOUND("TERMS-404", "존재하지 않는 약관입니다.", 404),
    REQUIRED_TERMS_NOT_AGREED("TERMS-400", "필수 약관에 모두 동의해야 합니다.", 400),
}
