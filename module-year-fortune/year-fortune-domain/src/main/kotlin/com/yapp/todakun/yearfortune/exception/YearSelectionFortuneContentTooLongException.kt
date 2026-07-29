package com.yapp.todakun.yearfortune.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.yearfortune.code.YearSelectionFortuneErrorCode

class YearSelectionFortuneContentTooLongException :
    BadRequestException(YearSelectionFortuneErrorCode.YEAR_SELECTION_FORTUNE_CONTENT_TOO_LONG)
