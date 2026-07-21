package com.yapp.todakun.auth.adapter.redis.withdrawn

import org.springframework.boot.context.properties.ConfigurationProperties

/** 재가입 제한 정책. [restrictionSeconds]는 탈퇴 후 SNS 식별자 보관·재가입 제한 기간(기본 90일). */
@ConfigurationProperties(prefix = "account.withdrawal")
data class WithdrawnAccountProperties(
    val restrictionSeconds: Long = 7_776_000L,
)
