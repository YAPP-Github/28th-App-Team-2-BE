package com.yapp.todakun.dayfortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.dayfortune.code.DaySelectionFortuneErrorCode

class DaySelectionFortuneTitleTooLongException :
    BadRequestException(DaySelectionFortuneErrorCode.DAY_SELECTION_FORTUNE_TITLE_TOO_LONG)
