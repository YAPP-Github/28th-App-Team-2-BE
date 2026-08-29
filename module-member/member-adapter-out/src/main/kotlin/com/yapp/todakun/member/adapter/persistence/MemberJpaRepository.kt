package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.shared.OauthProvider
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MemberJpaRepository : JpaRepository<MemberJpaEntity, UUID> {
    fun findByOauthProviderAndProviderId(
        oauthProvider: OauthProvider,
        providerId: String,
    ): MemberJpaEntity?

    // id(UUIDv7, 시간 정렬)를 keyset 커서로 써서 OFFSET 없이 다음 페이지를 조회한다.
    @Query("select m.id from MemberJpaEntity m where (:afterMemberId is null or m.id > :afterMemberId) order by m.id asc")
    fun findIdsAfter(
        afterMemberId: UUID?,
        limit: Limit,
    ): List<UUID>
}
