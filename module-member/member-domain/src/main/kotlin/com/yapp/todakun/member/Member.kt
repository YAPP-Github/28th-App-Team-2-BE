package com.yapp.todakun.member

import com.yapp.todakun.shared.OauthProvider
import java.time.LocalDate
import java.util.UUID

data class Member(
    val id: UUID,
    val name: String,
    val birthDate: LocalDate,
    val birthTime: BirthTime,
    val calendarType: CalendarType,
    val gender: Gender,
    val role: Role,
    val oauthProvider: OauthProvider,
    val providerId: String,
    val job: Job,
    val relationshipStatus: RelationshipStatus,
)
