package com.yapp.todakun.terms.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.terms.code.TermsErrorCode

class RequiredTermsNotAgreedException : BadRequestException(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED)
