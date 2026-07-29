package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** 궁합 요약이 최대 길이(200자)를 초과한 경우(400). */
class CompatibilitySummaryTooLongException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_SUMMARY_TOO_LONG)
