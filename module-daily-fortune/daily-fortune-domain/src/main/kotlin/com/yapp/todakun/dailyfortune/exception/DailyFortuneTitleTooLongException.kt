package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

class DailyFortuneTitleTooLongException : BadRequestException(DailyFortuneErrorCode.DAILY_FORTUNE_TITLE_TOO_LONG)
