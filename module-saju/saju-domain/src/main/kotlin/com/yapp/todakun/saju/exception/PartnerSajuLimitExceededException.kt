package com.yapp.todakun.saju.exception

import com.yapp.todakun.common.exception.ConflictException
import com.yapp.todakun.saju.code.SajuErrorCode

class PartnerSajuLimitExceededException : ConflictException(SajuErrorCode.SAJU_PARTNER_LIMIT_EXCEEDED)
