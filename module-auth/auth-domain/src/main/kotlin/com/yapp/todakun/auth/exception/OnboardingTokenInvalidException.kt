package com.yapp.todakun.auth.exception

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException

class OnboardingTokenInvalidException : UnauthorizedException(AuthErrorCode.ONBOARDING_TOKEN_INVALID)
