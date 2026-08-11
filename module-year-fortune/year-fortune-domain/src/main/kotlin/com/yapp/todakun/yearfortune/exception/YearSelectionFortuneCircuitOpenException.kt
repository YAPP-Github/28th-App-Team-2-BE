package com.yapp.todakun.yearfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.yearfortune.code.YearSelectionFortuneErrorCode

/**
 * AI 호출 CircuitBreaker가 열려 있어 연도별 운세를 생성할 수 없음(503) — 지속 장애로 판단해 곧장 fail-fast한 경우.
 * 응답 지연으로 끊긴 [YearSelectionFortuneTimeoutException]과 달리, 이번 호출은 시도조차 하지 않았다는 뜻이다.
 */
class YearSelectionFortuneCircuitOpenException(
    cause: Throwable? = null,
) : BusinessException(YearSelectionFortuneErrorCode.YEAR_SELECTION_FORTUNE_CIRCUIT_OPEN, cause)
