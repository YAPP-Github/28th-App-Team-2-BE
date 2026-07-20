package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.exception.SajuInputInvalidException

/** 도메인 경계를 넘어온 문자열 enum 값을 saju 도메인 enum으로 변환한다(실패 시 400). */
internal inline fun <reified T : Enum<T>> String.toSajuEnum(): T =
    runCatching { enumValueOf<T>(this.trim().uppercase()) }.getOrElse { throw SajuInputInvalidException() }
