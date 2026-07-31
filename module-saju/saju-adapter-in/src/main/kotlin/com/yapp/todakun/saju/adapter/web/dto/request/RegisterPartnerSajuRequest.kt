package com.yapp.todakun.saju.adapter.web.dto.request

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.port.inbound.RegisterPartnerSajuCommand
import com.yapp.todakun.web.validation.ValidEnum
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

/** 상대방 사주 등록 요청. */
data class RegisterPartnerSajuRequest(
    @field:Schema(description = "이름", example = "토실이")
    @field:NotBlank(message = "이름을 입력해 주세요.")
    @field:Size(min = 1, max = 10, message = "이름은 1~10자로 입력해 주세요.")
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "특수문자, 이모지, 공백은 사용할 수 없습니다.")
    val name: String,
    @field:Schema(description = "성별", example = "MALE")
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
    @field:Schema(description = "태어난 시간", example = "SINSI")
    @field:NotBlank(message = "태어난 시간을 입력해 주세요.")
    @field:ValidEnum(enumClass = BirthTime::class, message = "올바른 태어난 시간 값이 아닙니다.")
    val birthTime: String,
    @field:Schema(description = "관계 라벨", example = "LOVER")
    @field:NotBlank(message = "관계를 선택해 주세요.")
    @field:ValidEnum(enumClass = RelationshipType::class, message = "올바른 관계 값이 아닙니다.")
    val relationshipType: String,
) {
    fun toCommand(memberId: UUID) =
        RegisterPartnerSajuCommand(
            memberId = memberId,
            name = name,
            gender = gender,
            calendarType = calendarType,
            birthDate = birthDate,
            birthTime = birthTime,
            relationshipType = relationshipType,
        )
}
