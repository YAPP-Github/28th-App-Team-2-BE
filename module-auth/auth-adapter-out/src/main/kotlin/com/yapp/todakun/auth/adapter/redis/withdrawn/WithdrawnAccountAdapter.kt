package com.yapp.todakun.auth.adapter.redis.withdrawn

import com.yapp.todakun.auth.port.outbound.WithdrawnAccountPort
import com.yapp.todakun.shared.OauthProvider
import com.yapp.todakun.shared.RegisterWithdrawnAccountPort
import org.springframework.stereotype.Component
import java.security.MessageDigest

/**
 * 재가입 제한 어댑터. 탈퇴 시 SNS 식별자를 해시로 [restrictionSeconds] TTL과 함께 저장하고(member의
 * [RegisterWithdrawnAccountPort] 위임), 로그인 시 해당 해시 존재 여부로 제한을 판별한다([WithdrawnAccountPort]).
 * 식별자는 원본을 저장하지 않고 SHA-256 해시만 보관(비식별)하며, TTL 만료로 90일 후 자동 삭제된다.
 *
 * `account.withdrawal.restriction-enabled: false`이면 등록은 그대로 하되 제한 판별만 건너뛴다.
 */
@Component
class WithdrawnAccountAdapter(
    private val withdrawnAccountRepository: WithdrawnAccountRepository,
    private val withdrawnAccountProperties: WithdrawnAccountProperties,
) : WithdrawnAccountPort,
    RegisterWithdrawnAccountPort {
    override fun register(
        provider: OauthProvider,
        providerId: String,
    ) {
        withdrawnAccountRepository.save(
            WithdrawnAccount(id = hash(provider, providerId), ttl = withdrawnAccountProperties.restrictionSeconds),
        )
    }

    override fun isRestricted(
        provider: OauthProvider,
        providerId: String,
    ): Boolean =
        withdrawnAccountProperties.restrictionEnabled &&
            withdrawnAccountRepository.existsById(hash(provider, providerId))

    /** `provider:providerId`를 SHA-256으로 해시해 소문자 16진 문자열로 반환한다(원본 식별자 비저장). */
    private fun hash(
        provider: OauthProvider,
        providerId: String,
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest("${provider.name}:$providerId".toByteArray())
            .joinToString("") { "%02x".format(it) }
}
