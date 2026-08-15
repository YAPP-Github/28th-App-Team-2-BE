package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/**
 * AI 호출이 TimeLimiter 타임아웃에 걸려 궁합을 생성할 수 없음(504) — 실제로 호출은 갔지만 응답이 지연된 경우.
 * 곧장 차단된 [CompatibilityCircuitOpenException]과 구분해, 클라이언트가 재시도 안내 문구를 다르게 줄 수 있게 한다.
 */
class CompatibilityTimeoutException(
    cause: Throwable? = null,
) : BusinessException(CompatibilityErrorCode.COMPATIBILITY_TIMEOUT, cause)
