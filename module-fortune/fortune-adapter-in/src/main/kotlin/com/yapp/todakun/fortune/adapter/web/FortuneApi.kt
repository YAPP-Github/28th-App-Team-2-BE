package com.yapp.todakun.fortune.adapter.web

import com.yapp.todakun.fortune.adapter.web.dto.response.FortuneDetailResponse
import com.yapp.todakun.fortune.adapter.web.dto.response.TodayFortuneResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "Fortune", description = "오늘의 운세 API")
interface FortuneApi {
    @Operation(
        summary = "오늘의 운세 요약 조회",
        description =
            """
                오늘의 운세 요약을 조회한다.
                매일 06:00부터 다음날 05:59까지 같은 결과를 반환하며, 06:00 이전에 호출하면 전날 운세가 반환된다.
                아직 생성되지 않았다면(가입 직후 등) data가 없는 200 응답을 반환한다.
            """,
    )
    @GetMapping("api/v1/fortunes/today")
    fun getToday(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<TodayFortuneResponse?>>

    @Operation(
        summary = "오늘의 운세 상세 조회",
        description = "오늘의 운세를 단건 상세 조회한다.",
    )
    @GetMapping("api/v1/fortunes/{fortuneId}")
    fun getById(
        @Parameter(description = "오늘의 운세 ID", example = "018f0000-0000-7000-8000-000000000003")
        @PathVariable fortuneId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<FortuneDetailResponse>>
}
