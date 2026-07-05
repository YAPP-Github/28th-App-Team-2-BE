package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException

internal fun requireVerifiedEmail(
    email: String?,
    isVerified: Boolean,
): String = email?.takeIf { isVerified } ?: throw UnauthorizedException(AuthErrorCode.OAUTH_EMAIL_REQUIRED)
