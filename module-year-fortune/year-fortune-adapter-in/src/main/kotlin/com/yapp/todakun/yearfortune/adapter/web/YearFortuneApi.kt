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
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID

@Tag(name = "YearFortune", description = "연도별 운세 API")
interface YearFortuneApi {
    @Operation(
        summary = "연도별 운세 생성",
        description = "회원이 선택한 연도의 운세를 생성한다. 이미 생성된 연도라면 기존 결과를 반환한다(멱등).",
    )
    @PostMapping("api/v1/year-fortunes/{year}")
    fun create(
        @Parameter(description = "생성할 연도", example = "2026")
        @PathVariable year: Int,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<YearSelectionFortuneResponse>>

    @Operation(
        summary = "연도별 운세 단건 조회",
        description = "생성된 연도별 운세를 ID로 단건 조회한다. 공유하기 화면에서 사용한다.",
    )
    @GetMapping("api/v1/year-fortunes/{yearFortuneId}")
    fun getById(
        @Parameter(description = "연도별 운세 ID", example = "018f0000-0000-7000-8000-000000000004")
        @PathVariable yearFortuneId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<YearSelectionFortuneResponse>>
}
