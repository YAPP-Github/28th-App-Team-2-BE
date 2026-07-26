package com.yapp.todakun.dailyfortune.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.dailyfortune.code.DailyFortuneErrorCode

class DailyFortuneNotFoundException : NotFoundException(DailyFortuneErrorCode.DAILY_FORTUNE_NOT_FOUND)
