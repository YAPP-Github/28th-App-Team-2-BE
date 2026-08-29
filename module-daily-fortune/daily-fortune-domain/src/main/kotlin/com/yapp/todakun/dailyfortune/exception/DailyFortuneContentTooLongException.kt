package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

class DailyFortuneContentTooLongException : BadRequestException(DailyFortuneErrorCode.DAILY_FORTUNE_CONTENT_TOO_LONG)
