package com.yapp.todakun.fortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.fortune.code.DailyFortuneErrorCode

class DailyFortuneHistoryToOutOfRangeException : BadRequestException(DailyFortuneErrorCode.DAILY_FORTUNE_HISTORY_TO_OUT_OF_RANGE)
