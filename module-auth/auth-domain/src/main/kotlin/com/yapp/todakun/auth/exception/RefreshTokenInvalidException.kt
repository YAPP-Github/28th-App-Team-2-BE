package com.yapp.todakun.auth.exception

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException

class RefreshTokenInvalidException : UnauthorizedException(AuthErrorCode.REFRESH_TOKEN_INVALID)
