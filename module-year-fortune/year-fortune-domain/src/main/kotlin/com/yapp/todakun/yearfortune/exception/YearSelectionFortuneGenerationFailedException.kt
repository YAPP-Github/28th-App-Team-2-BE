package com.yapp.todakun.yearfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.yearfortune.code.YearSelectionFortuneErrorCode

/**
 * AI를 통한 연도별 운세 생성 실패(500).
 * 프롬프트 호출·구조화 매핑 실패 등 정상 흐름에서는 발생하면 안 되는 시스템 오류.
 */
class YearSelectionFortuneGenerationFailedException(
    cause: Throwable? = null,
) : BusinessException(YearSelectionFortuneErrorCode.YEAR_SELECTION_FORTUNE_GENERATION_FAILED, cause)
