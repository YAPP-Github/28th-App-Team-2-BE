package com.yapp.todakun.dailyfortune.adapter.web

import com.yapp.todakun.dailyfortune.adapter.web.dto.response.DailyFortuneHistoryResponse
import com.yapp.todakun.dailyfortune.adapter.web.dto.response.DailyFortuneResponse
import com.yapp.todakun.dailyfortune.adapter.web.dto.response.TodayFortuneResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.util.UUID

@Tag(name = "DailyFortune", description = "오늘의 운세 API")
interface DailyFortuneApi {
    @Operation(
        summary = "오늘의 운세 요약 조회",
        description =
            """
                오늘의 운세 요약을 조회한다.
                매일 06:00부터 다음날 05:59까지 같은 결과를 반환하며, 06:00 이전에 호출하면 전날 운세가 반환된다.
                온보딩 시 오늘의 운세가 항상 생성되므로, 존재하지 않으면 404를 반환한다.
            """,
    )
    @GetMapping("api/v1/daily-fortunes/today")
    fun getToday(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<TodayFortuneResponse>>

    @Operation(
        summary = "오늘의 운세 상세 조회",
        description = "오늘의 운세를 단건 상세 조회한다.",
    )
    @GetMapping("api/v1/daily-fortunes/{dailyFortuneId}")
    fun getById(
        @Parameter(description = "오늘의 운세 ID", example = "018f0000-0000-7000-8000-000000000003")
        @PathVariable dailyFortuneId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<DailyFortuneResponse>>

    @Operation(
        summary = "오늘의 운세 히스토리 조회",
        description =
            """
                to가 속한 달의 1일부터 to까지의 오늘의 운세 히스토리를 오래된 날짜순으로 조회한다.
                to는 지난달 1일부터 오늘까지만 허용한다(그 이전 조회 불가).
            """,
    )
    @GetMapping("api/v1/daily-fortunes/history")
    fun getHistory(
        @Parameter(description = "조회 종료일", example = "2026-07-22")
        @RequestParam to: LocalDate,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<List<DailyFortuneHistoryResponse>>>

    @Operation(
        summary = "오늘의 운세 배치 수동 실행(임시)",
        description =
            """
                운영 중 문제를 임시로 해결하기 위해 배치를 수동 실행하는 API다. 인증된 사용자라면 누구나 호출할 수 있다.
                전체 회원을 대상으로 오늘(KST) 날짜 기준 오늘의 운세 생성 배치를 실행한다.
                이미 완료된 배치는 재실행되지 않는다(JobInstance 재사용).
                배치가 끝날 때까지 응답이 지연될 수 있다(동기 실행).
            """,
    )
    @PostMapping("api/v1/daily-fortunes/generate")
    fun generate(): ResponseEntity<CommonResponse<Unit>>
}
