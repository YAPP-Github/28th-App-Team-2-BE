package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.Member
import com.yapp.todakun.member.exception.MemberAlreadyExistsException
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.OauthProvider
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MemberRepositoryAdapter(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberRepository {
    override fun save(member: Member): Member =
        try {
            memberJpaRepository.saveAndFlush(MemberJpaEntity.fromDomain(member)).toDomain()
        } catch (_: DataIntegrityViolationException) {
            throw MemberAlreadyExistsException()
        }

    override fun findById(id: UUID): Member? = memberJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findIdByOauth(
        oauthProvider: OauthProvider,
        providerId: String,
    ): UUID? = memberJpaRepository.findByOauthProviderAndProviderId(oauthProvider, providerId)?.id

    override fun findIdsAfter(
        afterMemberId: UUID?,
        limit: Int,
    ): List<UUID> = memberJpaRepository.findIdsAfter(afterMemberId, Limit.of(limit))

    override fun deleteById(id: UUID) {
        memberJpaRepository.deleteById(id)
    }
}
