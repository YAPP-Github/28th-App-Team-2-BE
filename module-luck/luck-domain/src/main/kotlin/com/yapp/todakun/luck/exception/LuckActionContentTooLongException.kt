package com.yapp.todakun.luck.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.luck.code.LuckActionErrorCode

class LuckActionContentTooLongException : BadRequestException(LuckActionErrorCode.LUCK_ACTION_CONTENT_TOO_LONG)
