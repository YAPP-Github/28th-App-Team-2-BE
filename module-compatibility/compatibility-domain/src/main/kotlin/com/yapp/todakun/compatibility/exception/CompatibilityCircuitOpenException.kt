package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/**
 * AI 호출 CircuitBreaker가 열려 있어 궁합을 생성할 수 없음(503) — 지속 장애로 판단해 곧장 fail-fast한 경우.
 * 응답 지연으로 끊긴 [CompatibilityTimeoutException]과 달리, 이번 호출은 시도조차 하지 않았다는 뜻이다.
 */
class CompatibilityCircuitOpenException(
    cause: Throwable? = null,
) : BusinessException(CompatibilityErrorCode.COMPATIBILITY_CIRCUIT_OPEN, cause)
