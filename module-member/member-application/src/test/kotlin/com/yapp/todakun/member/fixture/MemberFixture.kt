package com.yapp.todakun.member.fixture

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.Member
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.Role
import com.yapp.todakun.shared.OauthProvider
import java.time.LocalDate
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private const val MEMBER_NAME = "홍길동"
private const val PROVIDER_ID = "1234567890"

object MemberFixture {
    fun create(
        id: UUID = MEMBER_ID,
        name: String = MEMBER_NAME,
        birthDate: LocalDate = LocalDate.of(1999, 1, 1),
        birthTime: BirthTime = BirthTime.UNKNOWN,
        calendarType: CalendarType = CalendarType.SOLAR,
        gender: Gender = Gender.FEMALE,
        role: Role = Role.MEMBER,
        oauthProvider: OauthProvider = OauthProvider.GOOGLE,
        providerId: String = PROVIDER_ID,
        job: Job = Job.STUDENT,
        relationshipStatus: RelationshipStatus = RelationshipStatus.SOLO,
    ): Member =
        Member(
            id = id,
            name = name,
            birthDate = birthDate,
            birthTime = birthTime,
            calendarType = calendarType,
            gender = gender,
            role = role,
            oauthProvider = oauthProvider,
            providerId = providerId,
            job = job,
            relationshipStatus = relationshipStatus,
        )
}
