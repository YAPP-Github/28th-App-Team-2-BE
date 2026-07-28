package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

/**
 * AI가 오늘의 운세 구조화 응답을 비워서 반환한 경우(500).
 */
class DailyFortuneEmptyResponseException : BusinessException(DailyFortuneErrorCode.DAILY_FORTUNE_EMPTY_RESPONSE)
