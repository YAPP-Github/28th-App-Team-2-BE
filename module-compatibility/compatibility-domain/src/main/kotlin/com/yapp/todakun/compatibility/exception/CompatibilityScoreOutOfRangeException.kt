package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** AI가 생성한 궁합 점수가 0~100 범위를 벗어난 경우(400). */
class CompatibilityScoreOutOfRangeException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_SCORE_OUT_OF_RANGE)
