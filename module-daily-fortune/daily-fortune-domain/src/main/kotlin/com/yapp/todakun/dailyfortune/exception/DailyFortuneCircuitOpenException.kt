package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

/**
 * AI 호출 CircuitBreaker가 열려 있어 오늘의 운세를 생성할 수 없음(503) — 지속 장애로 판단해 곧장 fail-fast한 경우.
 * 응답 지연으로 끊긴 [DailyFortuneTimeoutException]과 달리, 이번 호출은 시도조차 하지 않았다는 뜻이다.
 * 배치에서는 재시도 없이 곧장 skip한다([com.yapp.todakun.dailyfortune.application.batch.GenerateDailyFortunesJobConfig] 참고).
 */
class DailyFortuneCircuitOpenException(
    cause: Throwable? = null,
) : BusinessException(DailyFortuneErrorCode.DAILY_FORTUNE_CIRCUIT_OPEN, cause)
