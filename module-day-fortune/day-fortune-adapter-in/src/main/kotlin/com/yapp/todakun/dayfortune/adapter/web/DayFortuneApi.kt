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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "DayFortune", description = "택일 운세 API")
interface DayFortuneApi {
    @Operation(
        summary = "택일 운세 생성",
        description = "회원이 선택한 목적과 후보 날짜(최대 3개)에 대한 택일 운세를 생성한다. 이미 생성된 (목적, 날짜) 조합이 있다면 기존 결과를 반환한다(멱등).",
    )
    @PostMapping("api/v1/day-fortunes")
    fun create(
        @Valid @RequestBody request: CreateDaySelectionFortuneRequest,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<List<DaySelectionFortuneResponse>>>
}
