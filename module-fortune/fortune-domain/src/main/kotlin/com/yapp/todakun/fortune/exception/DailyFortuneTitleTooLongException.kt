package com.yapp.todakun.fortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.fortune.code.DailyFortuneErrorCode

class DailyFortuneTitleTooLongException : BadRequestException(DailyFortuneErrorCode.DAILY_FORTUNE_TITLE_TOO_LONG)
