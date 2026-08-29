package com.yapp.todakun.dayfortune.adapter.web.dto

import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneInvalidPurposeException

/** 검증된 요청 문자열을 day-fortune 도메인 enum으로 변환한다(형식 검증은 Bean Validation에서, 방어적 변환은 여기서). */
internal inline fun <reified T : Enum<T>> String.toDayFortuneEnum(): T =
    runCatching { enumValueOf<T>(this) }.getOrElse { throw DaySelectionFortuneInvalidPurposeException() }
