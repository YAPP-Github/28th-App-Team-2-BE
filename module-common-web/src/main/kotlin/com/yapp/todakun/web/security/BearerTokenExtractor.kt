package com.yapp.todakun.web.security

private const val BEARER_PREFIX = "Bearer "

fun extractBearerToken(header: String?): String? = header?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)
