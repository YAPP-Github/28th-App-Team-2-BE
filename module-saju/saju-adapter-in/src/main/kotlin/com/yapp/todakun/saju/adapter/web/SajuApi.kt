package com.yapp.todakun.saju.adapter.web

import com.yapp.todakun.saju.adapter.web.dto.request.RegisterPartnerSajuRequest
import com.yapp.todakun.saju.adapter.web.dto.request.UpdatePartnerSajuRequest
import com.yapp.todakun.saju.adapter.web.dto.response.PartnerSajuSummaryResponse
import com.yapp.todakun.saju.adapter.web.dto.response.RegisterPartnerSajuResponse
import com.yapp.todakun.saju.adapter.web.dto.response.SajuChartDetailResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Saju", description = "사주(만세력·상대방) API")
interface SajuApi {
    @Operation(
        summary = "본인 만세력 상세 조회",
        description = "인증된 회원 본인의 사주 명식(사주원국·오행·십성 + 파생 지장간·십이신살)을 조회한다.",
    )
    @GetMapping("api/v1/saju/me")
    fun getMySaju(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<SajuChartDetailResponse>>

    @Operation(
        summary = "상대방 사주 등록",
        description = "상대방(궁합 상대 등)의 사주를 등록한다. 1인당 최대 10명까지 등록할 수 있다.",
    )
    @PostMapping("api/v1/saju/partners")
    fun registerPartner(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @RequestBody @Valid request: RegisterPartnerSajuRequest,
    ): ResponseEntity<CommonResponse<RegisterPartnerSajuResponse>>

    @Operation(
        summary = "상대방 사주 목록 조회",
        description = "인증된 회원이 등록한 상대방 사주 목록을 전체 조회한다.",
    )
    @GetMapping("api/v1/saju/partners")
    fun getPartners(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<List<PartnerSajuSummaryResponse>>>

    @Operation(
        summary = "상대방 명식 상세 조회",
        description = "등록한 상대방의 사주 명식 상세를 조회한다.",
    )
    @GetMapping("api/v1/saju/partners/{linkId}")
    fun getPartner(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @Parameter(description = "상대방 사주 링크 ID", example = "018f0000-0000-7000-8000-000000000001")
        @PathVariable linkId: UUID,
    ): ResponseEntity<CommonResponse<SajuChartDetailResponse>>

    @Operation(
        summary = "상대방 사주 수정",
        description = "상대방의 생년 정보·관계 라벨을 수정한다. 생년 정보가 바뀌면 명식이 재계산된다.",
    )
    @PatchMapping("api/v1/saju/partners/{linkId}")
    fun updatePartner(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @Parameter(description = "상대방 사주 링크 ID", example = "018f0000-0000-7000-8000-000000000001")
        @PathVariable linkId: UUID,
        @RequestBody @Valid request: UpdatePartnerSajuRequest,
    ): ResponseEntity<CommonResponse<Unit>>

    @Operation(
        summary = "상대방 사주 삭제",
        description = "등록한 상대방 사주를 삭제한다.",
    )
    @DeleteMapping("api/v1/saju/partners/{linkId}")
    fun deletePartner(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @Parameter(description = "상대방 사주 링크 ID", example = "018f0000-0000-7000-8000-000000000001")
        @PathVariable linkId: UUID,
    ): ResponseEntity<CommonResponse<Unit>>
}
