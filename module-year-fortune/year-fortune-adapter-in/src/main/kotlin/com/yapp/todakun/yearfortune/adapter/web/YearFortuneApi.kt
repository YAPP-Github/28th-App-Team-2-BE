package com.yapp.todakun.yearfortune.adapter.web

import com.yapp.todakun.web.response.CommonResponse
import com.yapp.todakun.yearfortune.adapter.web.dto.response.YearSelectionFortuneResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "YearFortune", description = "연도별 운세 API")
interface YearFortuneApi {
    @Operation(
        summary = "연도별 운세 조회",
        description = "회원이 선택한 연도의 운세를 조회한다.",
    )
    @GetMapping("api/v1/year-fortunes/{year}") // ID로 변경 예정
    fun getByYear(
        @Parameter(description = "조회할 연도", example = "2026")
        @PathVariable year: Int,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<YearSelectionFortuneResponse>>
}
