package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

/**
 * 같은 fortuneDate로 이미 실행 중인 배치가 있어 재실행할 수 없는 경우(409).
 */
class DailyFortuneBatchAlreadyRunningException(
    cause: Throwable? = null,
) : BusinessException(DailyFortuneErrorCode.DAILY_FORTUNE_BATCH_ALREADY_RUNNING, cause)
