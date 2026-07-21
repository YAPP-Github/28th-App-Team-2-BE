package com.yapp.todakun.auth.exception

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.ForbiddenException

/** 탈퇴 후 재가입 제한 기간(90일) 내에 동일 SNS 계정으로 재가입을 시도한 경우. */
class ReSignupRestrictedException : ForbiddenException(AuthErrorCode.RESIGNUP_RESTRICTED)
