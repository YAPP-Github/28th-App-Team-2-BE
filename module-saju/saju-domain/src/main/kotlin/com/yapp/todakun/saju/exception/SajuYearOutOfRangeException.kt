package com.yapp.todakun.saju.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.saju.code.SajuErrorCode

/** 지원 범위(1900~2050) 밖 출생연도(422). 공통 시맨틱 예외에 없는 상태라 BusinessException을 직접 상속한다. */
class SajuYearOutOfRangeException : BusinessException(SajuErrorCode.SAJU_YEAR_OUT_OF_RANGE)
