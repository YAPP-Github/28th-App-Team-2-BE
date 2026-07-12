package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.exception.MemberProfileInvalidException

internal inline fun <reified T : Enum<T>> String.toMemberEnum(): T =
    runCatching { enumValueOf<T>(this) }.getOrElse { throw MemberProfileInvalidException() }
