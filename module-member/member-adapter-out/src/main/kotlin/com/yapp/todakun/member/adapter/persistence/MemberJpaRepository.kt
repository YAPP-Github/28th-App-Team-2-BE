package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.shared.OauthProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MemberJpaRepository : JpaRepository<MemberJpaEntity, UUID> {
    fun findByOauthProviderAndProviderId(
        oauthProvider: OauthProvider,
        providerId: String,
    ): MemberJpaEntity?

    @Query("select m.id from MemberJpaEntity m")
    fun findIds(): List<UUID>
}
