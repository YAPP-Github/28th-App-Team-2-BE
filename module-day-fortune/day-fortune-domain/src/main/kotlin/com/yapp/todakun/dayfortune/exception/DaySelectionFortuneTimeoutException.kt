package com.yapp.todakun.dayfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dayfortune.code.DaySelectionFortuneErrorCode

/**
 * AI 호출이 Retry 소진 후에도 TimeLimiter 타임아웃에 걸려 택일 운세를 생성할 수 없음(504) — 실제로 호출은 갔지만 응답이 지연된 경우.
 * 곧장 차단된 [DaySelectionFortuneCircuitOpenException]과 구분한다.
 */
class DaySelectionFortuneTimeoutException(
    cause: Throwable? = null,
) : BusinessException(DaySelectionFortuneErrorCode.DAY_SELECTION_FORTUNE_TIMEOUT, cause)
