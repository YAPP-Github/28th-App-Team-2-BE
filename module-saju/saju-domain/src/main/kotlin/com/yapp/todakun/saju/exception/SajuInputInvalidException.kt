package com.yapp.todakun.saju.exception

import com.yapp.todakun.common.exception.BadRequestException
import com.yapp.todakun.saju.code.SajuErrorCode

class SajuInputInvalidException : BadRequestException(SajuErrorCode.SAJU_INPUT_INVALID)
