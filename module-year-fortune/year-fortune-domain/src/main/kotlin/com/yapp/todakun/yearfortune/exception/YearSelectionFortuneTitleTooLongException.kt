package com.yapp.todakun.yearfortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.yearfortune.code.YearSelectionFortuneErrorCode

class YearSelectionFortuneTitleTooLongException :
    BadRequestException(YearSelectionFortuneErrorCode.YEAR_SELECTION_FORTUNE_TITLE_TOO_LONG)
