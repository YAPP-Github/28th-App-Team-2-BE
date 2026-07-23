package com.yapp.todakun.fortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.fortune.code.DailyFortuneErrorCode

class DailyFortuneItemCountMismatchException : BadRequestException(DailyFortuneErrorCode.DAILY_FORTUNE_ITEM_COUNT_MISMATCH)
