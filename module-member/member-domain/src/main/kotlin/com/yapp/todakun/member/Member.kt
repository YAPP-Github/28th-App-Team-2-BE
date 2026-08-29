package com.yapp.todakun.member

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
) {
    /**
     * 내 정보 수정. 생년 정보(성별·양음력·생년월일·태어난 시간)와 관심 주제·현재 상황(직업·연애 상태)을 교체한다.
     * 이름·역할·소셜 로그인 식별자는 수정 대상이 아니다.
     */
    fun update(
        gender: Gender,
        calendarType: CalendarType,
        birthDate: LocalDate,
        birthTime: BirthTime,
        job: Job,
        relationshipStatus: RelationshipStatus,
    ): Member =
        copy(
            gender = gender,
            calendarType = calendarType,
            birthDate = birthDate,
            birthTime = birthTime,
            job = job,
            relationshipStatus = relationshipStatus,
        )

    /** 사주 명식 계산에 영향을 주는 입력(성별·양음력·생년월일·태어난 시간)이 [other]와 동일한지 여부. */
    fun sajuInputEquals(other: Member): Boolean =
        gender == other.gender &&
            calendarType == other.calendarType &&
            birthDate == other.birthDate &&
            birthTime == other.birthTime

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
}
