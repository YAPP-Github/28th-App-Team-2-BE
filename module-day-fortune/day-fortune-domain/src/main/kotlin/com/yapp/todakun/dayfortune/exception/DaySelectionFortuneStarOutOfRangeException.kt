package com.yapp.todakun.dayfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dayfortune.code.DaySelectionFortuneErrorCode

class DaySelectionFortuneStarOutOfRangeException :
    BusinessException(DaySelectionFortuneErrorCode.DAY_SELECTION_FORTUNE_STAR_OUT_OF_RANGE)
