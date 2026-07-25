package com.yapp.todakun.fortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.fortune.code.DailyFortuneErrorCode

class DailyFortuneContentTooLongException : BadRequestException(DailyFortuneErrorCode.DAILY_FORTUNE_CONTENT_TOO_LONG)
