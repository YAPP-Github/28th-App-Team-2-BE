package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.Member
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MemberRepositoryAdapter(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberRepository {
    override fun save(member: Member): Member = memberJpaRepository.save(MemberJpaEntity.fromDomain(member)).toDomain()

    override fun findById(id: UUID): Member? = memberJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findIdByOauth(
        oauthProvider: OauthProvider,
        providerId: String,
    ): UUID? = memberJpaRepository.findByOauthProviderAndProviderId(oauthProvider, providerId)?.toDomain()?.id
}
