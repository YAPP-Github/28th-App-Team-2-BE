package com.yapp.todakun.member.application

import com.yapp.todakun.member.exception.MemberProfileInvalidException

internal inline fun <reified T : Enum<T>> String.toMemberEnum(): T =
    try {
        enumValueOf<T>(this)
    } catch (_: IllegalArgumentException) {
        throw MemberProfileInvalidException()
    }
