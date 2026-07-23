package com.yapp.todakun.fortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.fortune.code.DailyFortuneErrorCode

class FortuneHistoryToOutOfRangeException : BadRequestException(DailyFortuneErrorCode.FORTUNE_HISTORY_TO_OUT_OF_RANGE)
