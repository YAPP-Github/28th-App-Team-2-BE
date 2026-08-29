package com.yapp.todakun.auth.adapter.redis.withdrawn

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 재가입 제한 정책. [restrictionSeconds]는 탈퇴 후 SNS 식별자 보관·재가입 제한 기간(기본 90일).
 *
 * [restrictionEnabled]가 false면 제한 판별만 건너뛰어 즉시 재가입을 허용한다(기본값 true = 정책 적용).
 * 탈퇴 기록(해시+TTL)은 끈 동안에도 그대로 남으므로, 다시 true로 되돌리면 남은 기간만큼 제한이 되살아난다.
 */
@ConfigurationProperties(prefix = "account.withdrawal")
data class WithdrawnAccountProperties(
    val restrictionSeconds: Long = 7_776_000L,
    val restrictionEnabled: Boolean = true,
)
