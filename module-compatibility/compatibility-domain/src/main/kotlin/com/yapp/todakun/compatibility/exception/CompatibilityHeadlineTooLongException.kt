package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** 궁합 헤드라인이 최대 길이(50자)를 초과한 경우(400). */
class CompatibilityHeadlineTooLongException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_HEADLINE_TOO_LONG)
