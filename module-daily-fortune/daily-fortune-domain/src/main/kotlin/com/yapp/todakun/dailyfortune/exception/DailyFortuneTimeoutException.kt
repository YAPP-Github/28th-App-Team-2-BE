package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

/**
 * AI 호출이 TimeLimiter 타임아웃에 걸려 오늘의 운세를 생성할 수 없음(504) — 실제로 호출은 갔지만 응답이 지연된 경우.
 * 곧장 차단된 [DailyFortuneCircuitOpenException]과 구분한다.
 * 배치에서는 재시도 없이 곧장 skip한다([com.yapp.todakun.dailyfortune.application.batch.GenerateDailyFortunesJobConfig] 참고).
 */
class DailyFortuneTimeoutException(
    cause: Throwable? = null,
) : BusinessException(DailyFortuneErrorCode.DAILY_FORTUNE_TIMEOUT, cause)
