package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.MemberFortuneProfile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetMemberFortuneProfileAdapter(
    private val memberRepository: MemberRepository,
) : GetMemberFortuneProfilePort {
    override fun getProfile(memberId: UUID): MemberFortuneProfile {
        val member = memberRepository.findById(memberId) ?: throw MemberNotFoundException()

        return MemberFortuneProfile(
            name = member.name,
            birthDate = member.birthDate,
            gender = member.gender.name,
            job = member.job.name,
            relationshipStatus = member.relationshipStatus.name,
            favoriteFortuneCategories = member.favoriteFortuneCategories.toList(),
        )
    }
}
