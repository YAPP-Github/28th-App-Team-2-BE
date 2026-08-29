package com.yapp.todakun.luck.adapter.web

import com.yapp.todakun.luck.adapter.web.dto.response.LuckActionListResponse
import com.yapp.todakun.luck.adapter.web.dto.response.LuckActionResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.util.UUID

@Tag(name = "LuckAction", description = "행운 액션 API")
interface LuckActionApi {
    @Operation(
        summary = "행운 액션 단건 조회",
        description = "행운 액션을 단건 조회한다.",
    )
    @GetMapping("api/v1/luck-actions/{luckActionId}")
    fun getById(
        @Parameter(description = "행운 액션 ID", example = "018f0000-0000-7000-8000-000000000001")
        @PathVariable luckActionId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<LuckActionResponse>>

    @Operation(
        summary = "오늘자 행운 액션 목록 조회",
        description = "오늘자 행운 액션 목록을 조회한다.",
    )
    @GetMapping("api/v1/luck-actions/today")
    fun getToday(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<List<LuckActionListResponse>>>

    @Operation(
        summary = "날짜별 행운 액션 목록 조회",
        description = "fortuneDate에 해당하는 행운 액션 목록을 조회한다.",
    )
    @GetMapping("api/v1/luck-actions")
    fun getByDate(
        @Parameter(description = "조회할 날짜", example = "2026-08-14")
        @RequestParam fortuneDate: LocalDate,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<List<LuckActionListResponse>>>

    @Operation(
        summary = "행운 액션 달성 여부 토글",
        description = "행운 액션의 달성 여부를 현재 상태의 반대로 전환한다.",
    )
    @PatchMapping("api/v1/luck-actions/{luckActionId}/achievement")
    fun toggle(
        @Parameter(description = "행운 액션 ID", example = "018f0000-0000-7000-8000-000000000001")
        @PathVariable luckActionId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<LuckActionResponse>>
}
