package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** AI가 빈 응답(null)을 반환한 경우(500). */
class CompatibilityEmptyResponseException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_EMPTY_RESPONSE)
