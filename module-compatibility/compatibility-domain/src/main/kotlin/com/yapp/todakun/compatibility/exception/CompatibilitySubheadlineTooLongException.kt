package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** 궁합 서브헤드라인이 최대 길이(100자)를 초과한 경우(400). */
class CompatibilitySubheadlineTooLongException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_SUBHEADLINE_TOO_LONG)
