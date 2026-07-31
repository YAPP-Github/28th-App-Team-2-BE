package com.yapp.todakun.auth.adapter.oauth

import com.nimbusds.jwt.JWTClaimsSet
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.common.exception.UnauthorizedException
import java.text.ParseException

internal fun requireVerifiedEmail(
    email: String?,
    isVerified: Boolean,
): String = email?.takeIf { isVerified } ?: throw UnauthorizedException(AuthErrorCode.OAUTH_EMAIL_REQUIRED)

// Google/Apple이 email_verified를 boolean이 아닌 문자열("true"/"false")로 내려주는 경우가 있어 타입을 직접 분기한다.
internal fun parseEmailVerified(claim: Any?): Boolean =
    when (claim) {
        is Boolean -> claim
        is String -> claim.toBoolean()
        else -> false
    }

internal fun extractIdTokenEmail(claims: JWTClaimsSet): String? =
    try {
        claims.getStringClaim("email")
    } catch (_: ParseException) {
        throw OauthTokenInvalidException()
    }
