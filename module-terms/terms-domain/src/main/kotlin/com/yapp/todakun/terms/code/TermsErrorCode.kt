package com.yapp.todakun.terms.code

import com.yapp.todakun.common.code.ResponseCode

enum class TermsErrorCode(
    override val code: String,
    override val message: String,
    override val status: Int,
) : ResponseCode {
    TERMS_NOT_FOUND("TERMS-404", "존재하지 않는 약관입니다.", 404),
    REQUIRED_TERMS_NOT_AGREED("TERMS-400", "필수 약관에 모두 동의해야 합니다.", 400),
    DUPLICATE_TERMS_AGREEMENT("TERMS-400", "동일한 약관을 중복해서 제출할 수 없습니다.", 400),
    TERMS_AGREEMENT_CONFLICT("TERMS-409", "약관 동의 처리 중 충돌이 발생했습니다. 다시 시도해 주세요.", 409),
}
