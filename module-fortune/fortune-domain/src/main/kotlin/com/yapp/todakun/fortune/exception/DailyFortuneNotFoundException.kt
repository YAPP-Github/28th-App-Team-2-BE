package com.yapp.todakun.fortune.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.fortune.code.DailyFortuneErrorCode

class DailyFortuneNotFoundException : NotFoundException(DailyFortuneErrorCode.DAILY_FORTUNE_NOT_FOUND)
