package com.yapp.todakun.saju.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.saju.code.SajuErrorCode

class SajuChartNotFoundException : NotFoundException(SajuErrorCode.SAJU_CHART_NOT_FOUND)
