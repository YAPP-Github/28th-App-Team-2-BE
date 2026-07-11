package com.yapp.todakun.member.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.Member
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.CreateMemberPort
import com.yapp.todakun.shared.OauthProvider
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@CommandService
class CreateMemberService(
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
            )

        return memberRepository.save(member).id
    }
}
