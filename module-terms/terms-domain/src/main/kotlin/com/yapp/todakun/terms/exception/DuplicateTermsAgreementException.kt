package com.yapp.todakun.terms.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.terms.code.TermsErrorCode

class DuplicateTermsAgreementException : BadRequestException(TermsErrorCode.DUPLICATE_TERMS_AGREEMENT)
