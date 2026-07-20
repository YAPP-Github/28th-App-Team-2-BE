package com.yapp.todakun.member.fixture

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.Member
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.Role
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.OauthProvider
import java.time.LocalDate
import java.util.UUID

object MemberFixture {
    val MEMBER_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")

    fun member(
        id: UUID = MEMBER_ID,
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1999, 1, 1),
        birthTime: BirthTime = BirthTime.UNKNOWN,
        calendarType: CalendarType = CalendarType.SOLAR,
        gender: Gender = Gender.FEMALE,
        job: Job = Job.STUDENT,
        relationshipStatus: RelationshipStatus = RelationshipStatus.SOLO,
        favoriteFortuneCategories: Set<FortuneCategory> = setOf(FortuneCategory.RELATIONSHIP, FortuneCategory.MONEY),
    ): Member =
        Member.reconstitute(
            id = id,
            name = name,
            birthDate = birthDate,
            birthTime = birthTime,
            calendarType = calendarType,
            gender = gender,
            role = Role.MEMBER,
            oauthProvider = OauthProvider.GOOGLE,
            providerId = "1234567890",
            job = job,
            relationshipStatus = relationshipStatus,
            favoriteFortuneCategories = favoriteFortuneCategories,
        )
}
