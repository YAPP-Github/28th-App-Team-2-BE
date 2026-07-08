package com.yapp.todakun.member.fixture

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.Member
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.Role
import com.yapp.todakun.shared.OAuthProvider
import java.time.LocalDate
import java.util.UUID

private val FIXED_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")

object MemberFixture {
    fun create(
        id: UUID = FIXED_ID,
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1999, 1, 1),
        birthTime: BirthTime = BirthTime.UNKNOWN,
        calendarType: CalendarType = CalendarType.SOLAR,
        gender: Gender = Gender.FEMALE,
        role: Role = Role.MEMBER,
        oauthProvider: OAuthProvider = OAuthProvider.GOOGLE,
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
            job = job,
            relationshipStatus = relationshipStatus,
        )
}
