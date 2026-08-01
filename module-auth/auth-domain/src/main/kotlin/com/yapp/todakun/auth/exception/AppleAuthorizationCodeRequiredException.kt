package com.yapp.todakun.auth.exception

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException

class AppleAuthorizationCodeRequiredException : UnauthorizedException(AuthErrorCode.APPLE_AUTHORIZATION_CODE_REQUIRED)
