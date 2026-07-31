package com.yapp.todakun.member.adapter.web.dto.request

import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.adapter.web.dto.toMemberEnum
import com.yapp.todakun.member.port.inbound.UpdateMemberCommand
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.web.validation.ValidEnum
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

/**
 * 내 정보 수정 요청. 이름은 수정 대상이 아니다(폼에 없음). 관심 주제·현재 상황(직업·연애 상태)과
 * 생년 정보(성별·양음력·생년월일·태어난 시간)를 수정한다.
 */
data class UpdateMemberRequest(
    @field:Schema(description = "성별", example = "FEMALE")
    @field:NotBlank(message = "성별을 입력해 주세요.")
    @field:ValidEnum(enumClass = Gender::class, message = "올바른 성별 값이 아닙니다.")
    val gender: String,
    @field:Schema(description = "날짜 유형", example = "SOLAR")
    @field:NotBlank(message = "날짜 유형을 입력해 주세요.")
    @field:ValidEnum(enumClass = CalendarType::class, message = "올바른 날짜 유형 값이 아닙니다.")
    val calendarType: String,
    @field:Schema(description = "생년월일", example = "1999-02-13")
    @field:NotNull(message = "생년월일을 입력해 주세요.")
    @field:PastOrPresent(message = "올바른 생년월일을 입력해 주세요.")
    val birthDate: LocalDate,
    @field:Schema(description = "태어난 시간", example = "JASI")
    @field:NotBlank(message = "태어난 시간을 입력해 주세요.")
    @field:ValidEnum(enumClass = BirthTime::class, message = "올바른 태어난 시간 값이 아닙니다.")
    val birthTime: String,
    @field:Schema(description = "직업", example = "WORKER")
    @field:NotBlank(message = "직업을 입력해 주세요.")
    @field:ValidEnum(enumClass = Job::class, message = "올바른 직업 값이 아닙니다.")
    val job: String,
    @field:Schema(description = "연애 상태", example = "SOLO")
    @field:NotBlank(message = "연애 상태를 입력해 주세요.")
    @field:ValidEnum(enumClass = RelationshipStatus::class, message = "올바른 연애 상태 값이 아닙니다.")
    val relationshipStatus: String,
    @field:Schema(description = "관심 운세 카테고리 목록", example = """["RELATIONSHIP", "MONEY"]""")
    @field:Size(min = 1, max = 5, message = "관심 운세는 1~5개 선택해야 합니다.")
    @field:ValidEnum(enumClass = FortuneCategory::class, message = "올바른 관심 운세 값이 아닙니다.")
    val favoriteFortuneCategories: List<String>,
) {
    fun toCommand(memberId: UUID) =
        UpdateMemberCommand(
            memberId = memberId,
            gender = gender.toMemberEnum<Gender>(),
            calendarType = calendarType.toMemberEnum<CalendarType>(),
            birthDate = birthDate,
            birthTime = birthTime.toMemberEnum<BirthTime>(),
            job = job.toMemberEnum<Job>(),
            relationshipStatus = relationshipStatus.toMemberEnum<RelationshipStatus>(),
            favoriteFortuneCategories = favoriteFortuneCategories.map { it.toMemberEnum<FortuneCategory>() }.toSet(),
        )
}
