package com.yapp.todakun.member

import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.OauthProvider
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

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
    val favoriteFortuneCategories: Set<FortuneCategory>,
) {
    companion object {
        @ExperimentalUuidApi
        fun create(
            name: String,
            birthDate: LocalDate,
            birthTime: BirthTime,
            calendarType: CalendarType,
            gender: Gender,
            oauthProvider: OauthProvider,
            providerId: String,
            job: Job,
            relationshipStatus: RelationshipStatus,
            favoriteFortuneCategories: Set<FortuneCategory>,
        ): Member =
            Member(
                id = Uuid.generateV7().toJavaUuid(),
                name = name,
                birthDate = birthDate,
                birthTime = birthTime,
                calendarType = calendarType,
                gender = gender,
                role = Role.MEMBER,
                oauthProvider = oauthProvider,
                providerId = providerId,
                job = job,
                relationshipStatus = relationshipStatus,
                favoriteFortuneCategories = favoriteFortuneCategories,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            name: String,
            birthDate: LocalDate,
            birthTime: BirthTime,
            calendarType: CalendarType,
            gender: Gender,
            role: Role,
            oauthProvider: OauthProvider,
            providerId: String,
            job: Job,
            relationshipStatus: RelationshipStatus,
            favoriteFortuneCategories: Set<FortuneCategory>,
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
                favoriteFortuneCategories = favoriteFortuneCategories,
            )
    }
}
