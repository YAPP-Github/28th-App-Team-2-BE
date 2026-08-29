package com.yapp.todakun.dayfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dayfortune.code.DaySelectionFortuneErrorCode

/**
 * AI가 택일 운세 구조화 응답을 비워서 반환한 경우(500).
 */
class DaySelectionFortuneEmptyResponseException :
    BusinessException(DaySelectionFortuneErrorCode.DAY_SELECTION_FORTUNE_EMPTY_RESPONSE)
