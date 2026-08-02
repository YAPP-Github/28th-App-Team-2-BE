package com.yapp.todakun.auth.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AppleOauthCredentialJpaRepository : JpaRepository<AppleOauthCredentialJpaEntity, UUID> {
    fun findByProviderId(providerId: String): AppleOauthCredentialJpaEntity?

    fun deleteByProviderId(providerId: String)
}
