package com.yapp.todakun.luck.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.luck.code.LuckActionErrorCode

class LuckActionTitleTooLongException : BadRequestException(LuckActionErrorCode.LUCK_ACTION_TITLE_TOO_LONG)
