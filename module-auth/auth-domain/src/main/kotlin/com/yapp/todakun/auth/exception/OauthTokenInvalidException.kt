package com.yapp.todakun.auth.exception

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException

class OauthTokenInvalidException : UnauthorizedException(AuthErrorCode.OAUTH_TOKEN_INVALID)
