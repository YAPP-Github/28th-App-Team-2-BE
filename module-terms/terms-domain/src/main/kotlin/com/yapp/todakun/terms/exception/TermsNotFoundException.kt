package com.yapp.todakun.terms.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.terms.code.TermsErrorCode

class TermsNotFoundException : NotFoundException(TermsErrorCode.TERMS_NOT_FOUND)
