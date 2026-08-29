package com.yapp.todakun.auth.adapter.web.dto.request

import com.yapp.todakun.auth.port.inbound.SignupCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class SignupRequest(
    @field:Schema(description = "온보딩 토큰", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @field:NotBlank(message = "온보딩 토큰을 입력해 주세요.")
    val onboardingToken: String,
    @field:Schema(description = "이름", example = "토닥이")
    @field:NotBlank(message = "이름을 입력해 주세요.")
    @field:Size(min = 1, max = 10, message = "이름은 1~10자로 입력해 주세요.")
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "특수문자, 이모지, 공백은 사용할 수 없습니다.")
    val name: String,
    @field:Schema(description = "생년월일", example = "2000-01-01")
    @field:NotNull(message = "생년월일을 입력해 주세요.")
    @field:PastOrPresent(message = "올바른 생년월일을 입력해 주세요.")
    val birthDate: LocalDate,
    @field:Schema(
        description = "태어난 시간(십이시진). 모르면 UNKNOWN을 보낸다 — 명식은 00:00(자시) 기준으로 계산되고 조회 응답에는 isTimeUnknown=true로 내려간다.",
        example = "JASI",
    )
    @field:NotBlank(message = "태어난 시간을 입력해 주세요.")
    @field:Pattern(
        regexp = "^(JASI|CHUKSI|INSI|MYOSI|JINSI|SASI|OSI|MISI|SINSI|YUSI|SULSI|HAESI|UNKNOWN)$",
        message = "올바른 태어난 시간 값이 아닙니다.",
    )
    val birthTime: String,
    @field:Schema(description = "날짜 유형", example = "SOLAR")
    @field:NotBlank(message = "날짜 유형을 입력해 주세요.")
    @field:Pattern(regexp = "^(SOLAR|LUNAR)$", message = "올바른 날짜 유형 값이 아닙니다.")
    val calendarType: String,
    @field:Schema(description = "성별", example = "MALE")
    @field:NotBlank(message = "성별을 입력해 주세요.")
    @field:Pattern(regexp = "^(MALE|FEMALE)$", message = "올바른 성별 값이 아닙니다.")
    val gender: String,
    @field:Schema(description = "직업", example = "STUDENT")
    @field:NotBlank(message = "직업을 입력해 주세요.")
    @field:Pattern(regexp = "^(STUDENT|WORKER|FREELANCER|JOBSEEKER|HOMEMAKER|LEAVER)$", message = "올바른 직업 값이 아닙니다.")
    val job: String,
    @field:Schema(description = "연애 상태", example = "SOLO")
    @field:NotBlank(message = "연애 상태를 입력해 주세요.")
    @field:Pattern(regexp = "^(SOLO|DATING|MARRY|REMARRY)$", message = "올바른 연애 상태 값이 아닙니다.")
    val relationshipStatus: String,
) {
    fun toCommand() =
        SignupCommand(
            onboardingToken = onboardingToken,
            name = name,
            birthDate = birthDate,
            birthTime = birthTime,
            calendarType = calendarType,
            gender = gender,
            job = job,
            relationshipStatus = relationshipStatus,
        )
}
