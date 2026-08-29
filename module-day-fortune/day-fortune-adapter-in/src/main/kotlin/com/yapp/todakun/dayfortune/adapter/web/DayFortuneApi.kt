package com.yapp.todakun.dayfortune.adapter.web

import com.yapp.todakun.dayfortune.adapter.web.dto.request.CreateDaySelectionFortuneRequest
import com.yapp.todakun.dayfortune.adapter.web.dto.response.DaySelectionFortuneResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "DayFortune", description = "택일 운세 API")
interface DayFortuneApi {
    @Operation(
        summary = "택일 운세 생성",
        description = "회원이 선택한 목적과 후보 날짜(최대 5개)에 대한 택일 운세를 생성한다. 이미 생성된 (목적, 날짜) 조합이 있다면 기존 결과를 반환한다(멱등).",
    )
    @PostMapping("api/v1/day-fortunes")
    fun create(
        @Valid @RequestBody request: CreateDaySelectionFortuneRequest,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<List<DaySelectionFortuneResponse>>>

    @Operation(
        summary = "택일 운세 단건 조회",
        description = "생성된 택일 운세를 ID로 단건 조회한다. 공유하기 화면에서 사용한다.",
    )
    @GetMapping("api/v1/day-fortunes/{dayFortuneId}")
    fun getById(
        @Parameter(description = "택일 운세 ID", example = "018f0000-0000-7000-8000-000000000004")
        @PathVariable dayFortuneId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<DaySelectionFortuneResponse>>
}
