package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** 두 명식의 오행 글자 수 합이 0이라 오행 비율을 계산할 수 없는 경우(500, 정상 흐름에서는 발생하면 안 됨). */
class CompatibilityOhaengEmptyException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_OHAENG_EMPTY)
