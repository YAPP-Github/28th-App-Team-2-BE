package com.yapp.todakun.dayfortune.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.dayfortune.code.DaySelectionFortuneErrorCode

class DaySelectionFortuneCategoryDuplicatedException :
    BusinessException(DaySelectionFortuneErrorCode.DAY_SELECTION_FORTUNE_CATEGORY_DUPLICATED)
