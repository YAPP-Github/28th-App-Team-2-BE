package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/**
 * AI를 통한 궁합 총운 생성 실패(500).
 * 프롬프트 호출·구조화 매핑 실패 등 정상 흐름에서는 발생하면 안 되는 시스템 오류.
 */
class CompatibilityGenerationFailedException(
    cause: Throwable? = null,
) : BusinessException(CompatibilityErrorCode.COMPATIBILITY_GENERATION_FAILED, cause)
