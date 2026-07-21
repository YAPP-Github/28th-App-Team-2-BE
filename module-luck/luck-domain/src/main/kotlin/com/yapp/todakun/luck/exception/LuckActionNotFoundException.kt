package com.yapp.todakun.luck.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.luck.code.LuckActionErrorCode

class LuckActionNotFoundException : NotFoundException(LuckActionErrorCode.LUCK_ACTION_NOT_FOUND)
