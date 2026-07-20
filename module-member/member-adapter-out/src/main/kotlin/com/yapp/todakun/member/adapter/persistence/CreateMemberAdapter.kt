package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.Member
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.CreateMemberPort
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.OauthProvider
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@Component
class CreateMemberAdapter(
    private val memberRepository: MemberRepository,
) : CreateMemberPort {
    @ExperimentalUuidApi
    override fun create(
        provider: OauthProvider,
        providerId: String,
        name: String,
        birthDate: LocalDate,
        birthTime: String,
        calendarType: String,
        gender: String,
        job: String,
        relationshipStatus: String,
        favoriteFortuneCategories: List<String>,
    ): UUID {
        val member =
            Member.create(
                name = name,
                birthDate = birthDate,
                birthTime = birthTime.toMemberEnum<BirthTime>(),
                calendarType = calendarType.toMemberEnum<CalendarType>(),
                gender = gender.toMemberEnum<Gender>(),
                oauthProvider = provider,
                providerId = providerId,
                job = job.toMemberEnum<Job>(),
                relationshipStatus = relationshipStatus.toMemberEnum<RelationshipStatus>(),
                favoriteFortuneCategories = favoriteFortuneCategories.map { it.toMemberEnum<FortuneCategory>() }.toSet(),
            )

        return memberRepository.save(member).id
    }
}
