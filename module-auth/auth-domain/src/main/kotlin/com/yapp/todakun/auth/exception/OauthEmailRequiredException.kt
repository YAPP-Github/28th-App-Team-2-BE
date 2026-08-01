package com.yapp.todakun.auth.exception

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException

class OauthEmailRequiredException : UnauthorizedException(AuthErrorCode.OAUTH_EMAIL_REQUIRED)
