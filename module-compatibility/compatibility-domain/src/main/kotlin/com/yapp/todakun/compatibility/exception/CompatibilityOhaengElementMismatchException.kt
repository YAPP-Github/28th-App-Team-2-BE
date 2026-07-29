package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** 궁합 오행 비율이 5개 오행을 모두 담지 못한 경우(400). */
class CompatibilityOhaengElementMismatchException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_OHAENG_ELEMENT_MISMATCH)
