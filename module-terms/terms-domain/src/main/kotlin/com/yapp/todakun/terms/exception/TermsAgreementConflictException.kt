package com.yapp.todakun.terms.exception

import com.yapp.todakun.common.exception.ConflictException
import com.yapp.todakun.terms.code.TermsErrorCode

class TermsAgreementConflictException : ConflictException(TermsErrorCode.TERMS_AGREEMENT_CONFLICT)
