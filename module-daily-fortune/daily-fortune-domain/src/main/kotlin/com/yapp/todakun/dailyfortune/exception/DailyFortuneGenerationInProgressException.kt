package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

/**
 * 같은 (memberId, fortuneDate)로 이미 다른 호출자가 생성 중이라 이 호출은 생성을 시작할 수 없는 경우(409).
 */
class DailyFortuneGenerationInProgressException(
    cause: Throwable? = null,
) : BusinessException(DailyFortuneErrorCode.DAILY_FORTUNE_GENERATION_IN_PROGRESS, cause)
