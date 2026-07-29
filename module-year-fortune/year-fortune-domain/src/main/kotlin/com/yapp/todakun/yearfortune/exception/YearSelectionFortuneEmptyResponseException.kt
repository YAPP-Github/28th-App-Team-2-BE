package com.yapp.todakun.yearfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.yearfortune.code.YearSelectionFortuneErrorCode

/**
 * AI가 연도별 운세 구조화 응답을 비워서 반환한 경우(500).
 */
class YearSelectionFortuneEmptyResponseException :
    BusinessException(YearSelectionFortuneErrorCode.YEAR_SELECTION_FORTUNE_EMPTY_RESPONSE)
