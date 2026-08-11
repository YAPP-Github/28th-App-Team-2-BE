package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.NotFoundException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

class CompatibilityNotFoundException : NotFoundException(CompatibilityErrorCode.COMPATIBILITY_NOT_FOUND)
