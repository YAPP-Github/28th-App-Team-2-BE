package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** 오행 글자 수 입력에 음수가 포함된 경우(500, 상위 계산 결과 불변식 위반이라 정상 흐름에서는 발생하면 안 됨). */
class CompatibilityOhaengCountNegativeException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_OHAENG_COUNT_NEGATIVE)
