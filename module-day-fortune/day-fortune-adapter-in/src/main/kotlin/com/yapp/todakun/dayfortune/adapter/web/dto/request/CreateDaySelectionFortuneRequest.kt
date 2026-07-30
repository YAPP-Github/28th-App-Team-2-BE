package com.yapp.todakun.dayfortune.adapter.web.dto.request

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.adapter.web.dto.toDayFortuneEnum
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

/** 택일 운세 생성 요청. */
data class CreateDaySelectionFortuneRequest(
    @field:Schema(description = "택일 목적", example = "TRAVEL")
    @field:NotBlank(message = "목적을 선택해 주세요.")
    @field:Pattern(
        regexp = "^(CONTRACT_MOVING|BUSINESS_OPENING|TRAVEL|CONFESSION_DATING|EXAM_INTERVIEW)$",
        message = "올바른 목적 값이 아닙니다.",
    )
    val purpose: String,
    @field:Schema(description = "후보 날짜 목록(1~5개, 과거 날짜 불가)", example = """["2026-08-01", "2026-08-15"]""")
    @field:Size(min = 1, max = 5, message = "후보 날짜는 1~5개 선택해야 합니다.")
    val targetDates: List<@NotNull LocalDate>,
) {
    fun toPurpose(): DaySelectionPurpose = purpose.toDayFortuneEnum()
}
