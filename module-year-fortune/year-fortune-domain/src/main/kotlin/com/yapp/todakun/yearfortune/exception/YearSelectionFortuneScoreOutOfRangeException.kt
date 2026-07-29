package com.yapp.todakun.yearfortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.yearfortune.code.YearSelectionFortuneErrorCode

class YearSelectionFortuneScoreOutOfRangeException :
    BadRequestException(YearSelectionFortuneErrorCode.YEAR_SELECTION_FORTUNE_SCORE_OUT_OF_RANGE)
