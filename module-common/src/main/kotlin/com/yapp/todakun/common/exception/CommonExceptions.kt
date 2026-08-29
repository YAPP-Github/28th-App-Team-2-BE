package com.yapp.todakun.common.exception

import com.yapp.todakun.common.code.ResponseCode

/** 잘못된 요청(400). 도메인 예외가 상속해 의미를 좁힌다. */
open class BadRequestException(errorCode: ResponseCode) : BusinessException(errorCode)

/** 인증 실패(401). */
open class UnauthorizedException(errorCode: ResponseCode) : BusinessException(errorCode)

/** 권한 없음(403). */
open class ForbiddenException(errorCode: ResponseCode) : BusinessException(errorCode)

/** 리소스 없음(404). */
open class NotFoundException(errorCode: ResponseCode) : BusinessException(errorCode)

/** 충돌(409). */
open class ConflictException(errorCode: ResponseCode) : BusinessException(errorCode)
