package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

/**
 * 마지막 배치 실행이 이미 완료됐거나 실행 중이라 재시도할 수 없는 경우(409).
 */
class DailyFortuneBatchNotRestartableException(
    cause: Throwable? = null,
) : BusinessException(DailyFortuneErrorCode.DAILY_FORTUNE_BATCH_NOT_RESTARTABLE, cause)
