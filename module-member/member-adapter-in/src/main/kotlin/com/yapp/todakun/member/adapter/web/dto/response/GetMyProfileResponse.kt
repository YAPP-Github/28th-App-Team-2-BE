package com.yapp.todakun.member.adapter.web.dto.response

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.Member
import java.time.LocalDate
import java.util.UUID

/** 내 프로필 조회 응답. 마이페이지·내 정보 수정 화면 프리필에 필요한 회원 정보를 담는다. */
data class GetMyProfileResponse(
    val id: UUID,
    val name: String,
    val gender: String,
    val birthDate: LocalDate,
    val birthTime: String,
    val isTimeUnknown: Boolean,
    val calendarType: String,
    val job: String,
    val relationshipStatus: String,
    val favoriteFortuneCategories: List<String>,
) {
    companion object {
        fun from(member: Member): GetMyProfileResponse =
            GetMyProfileResponse(
                id = member.id,
                name = member.name,
                gender = member.gender.name,
                birthDate = member.birthDate,
                birthTime = member.birthTime.name,
                isTimeUnknown = member.birthTime == BirthTime.UNKNOWN,
                calendarType = member.calendarType.name,
                job = member.job.name,
                relationshipStatus = member.relationshipStatus.name,
                favoriteFortuneCategories = member.favoriteFortuneCategories.map { it.name },
            )
    }
}
