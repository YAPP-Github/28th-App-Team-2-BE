package com.yapp.todakun.saju.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.saju.code.SajuErrorCode

/** 사주 명식 계산 내부 오류(500). 리소스 누락·십성 판정 불가 등 정상 흐름에서는 발생하면 안 되는 시스템 오류. */
class SajuCalculationException : BusinessException(SajuErrorCode.SAJU_CALCULATION_FAILED)
