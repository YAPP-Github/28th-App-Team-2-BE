package com.yapp.todakun.dayfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dayfortune.code.DaySelectionFortuneErrorCode

class DaySelectionFortuneTitleTooLongException :
    BusinessException(DaySelectionFortuneErrorCode.DAY_SELECTION_FORTUNE_TITLE_TOO_LONG)
