package com.yapp.todakun.auth.adapter.persistence

import com.yapp.todakun.auth.AppleOauthCredential
import com.yapp.todakun.auth.port.outbound.AppleOauthCredentialPort
import org.springframework.dao.DataIntegrityViolationException
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
    ): AppleOauthCredential =
        try {
            upsert(providerId, clientId, refreshToken)
        } catch (exception: DataIntegrityViolationException) {
            // 동시 로그인으로 두 요청이 동시에 조회 → 삽입을 시도하면 provider_id 유니크 제약 위반이 발생할 수 있다.
            // 이미 다른 요청이 삽입을 마쳤다는 뜻이므로, 재조회 후 갱신으로 복구한다.
            val existing =
                appleOauthCredentialJpaRepository.findByProviderId(providerId)
                    ?: throw exception
            val updated = existing.toDomain().copy(clientId = clientId, refreshToken = refreshToken)
            appleOauthCredentialJpaRepository.save(AppleOauthCredentialJpaEntity.fromDomain(updated)).toDomain()
        }

    @ExperimentalUuidApi
    private fun upsert(
        providerId: String,
        clientId: String,
        refreshToken: String,
    ): AppleOauthCredential {
        val credential =
            appleOauthCredentialJpaRepository.findByProviderId(providerId)
                ?.toDomain()
                ?.copy(clientId = clientId, refreshToken = refreshToken)
                ?: AppleOauthCredential.create(providerId, clientId, refreshToken)

        return appleOauthCredentialJpaRepository.saveAndFlush(AppleOauthCredentialJpaEntity.fromDomain(credential)).toDomain()
    }

    override fun find(providerId: String): AppleOauthCredential? =
        appleOauthCredentialJpaRepository.findByProviderId(
            providerId,
        )?.toDomain()

    override fun delete(providerId: String) {
        appleOauthCredentialJpaRepository.deleteByProviderId(providerId)
    }
}
