package com.yapp.todakun.auth.adapter.persistence

import com.yapp.todakun.auth.AppleOauthCredential
import com.yapp.todakun.auth.port.outbound.AppleOauthCredentialPort
import org.springframework.stereotype.Component
import kotlin.uuid.ExperimentalUuidApi

/** Apple refresh token 영속 어댑터. 동일 providerId로 재로그인하면 기존 행을 갱신한다(upsert). */
@Component
class AppleOauthCredentialAdapter(
    private val appleOauthCredentialJpaRepository: AppleOauthCredentialJpaRepository,
) : AppleOauthCredentialPort {
    @ExperimentalUuidApi
    override fun save(
        providerId: String,
        clientId: String,
        refreshToken: String,
    ) {
        val credential =
            appleOauthCredentialJpaRepository.findByProviderId(providerId)
                ?.toDomain()
                ?.copy(clientId = clientId, refreshToken = refreshToken)
                ?: AppleOauthCredential.create(providerId, clientId, refreshToken)

        appleOauthCredentialJpaRepository.save(AppleOauthCredentialJpaEntity.fromDomain(credential))
    }

    override fun find(providerId: String): AppleOauthCredential? =
        appleOauthCredentialJpaRepository.findByProviderId(
            providerId,
        )?.toDomain()

    override fun delete(providerId: String) {
        appleOauthCredentialJpaRepository.deleteByProviderId(providerId)
    }
}
