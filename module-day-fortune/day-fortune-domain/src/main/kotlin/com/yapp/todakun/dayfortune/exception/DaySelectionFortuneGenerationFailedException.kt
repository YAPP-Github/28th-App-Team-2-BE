package com.yapp.todakun.dayfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dayfortune.code.DaySelectionFortuneErrorCode

/**
 * AI를 통한 택일 운세 생성 실패(500).
 * 프롬프트 호출·구조화 매핑 실패 등 정상 흐름에서는 발생하면 안 되는 시스템 오류.
 */
class DaySelectionFortuneGenerationFailedException(
    cause: Throwable? = null,
) : BusinessException(DaySelectionFortuneErrorCode.DAY_SELECTION_FORTUNE_GENERATION_FAILED, cause)
