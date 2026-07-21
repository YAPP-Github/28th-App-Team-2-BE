package com.yapp.todakun.member.adapter.web

import com.yapp.todakun.member.adapter.web.dto.request.UpdateMemberRequest
import com.yapp.todakun.member.adapter.web.dto.request.WithdrawRequest
import com.yapp.todakun.member.adapter.web.dto.response.GetMyProfileResponse
import com.yapp.todakun.web.response.CommonResponse
import com.yapp.todakun.web.security.annotation.BearerToken
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Member", description = "회원 API")
interface MemberApi {
    @Operation(
        summary = "내 정보 조회",
        description = "인증된 회원 본인의 프로필(생년 정보·직업·연애 상태·관심 주제)을 조회한다.",
    )
    @GetMapping("api/v1/members/me")
    fun getMyProfile(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<GetMyProfileResponse>>

    @Operation(
        summary = "내 정보 수정",
        description = "생년 정보·직업·연애 상태·관심 주제를 수정한다. 생년 정보가 바뀌면 본인 사주 명식이 재계산된다.",
    )
    @PatchMapping("api/v1/members/me")
    fun updateMyProfile(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @RequestBody @Valid request: UpdateMemberRequest,
    ): ResponseEntity<CommonResponse<Unit>>

    @Operation(
        summary = "회원 탈퇴",
        description =
            "회원을 탈퇴(하드 삭제)한다. 복구 유예 없이 즉시 확정 처리되며, 회원·사주 데이터·인증 토큰은 모두 삭제된다. " +
                "탈퇴 사유는 계정 식별 정보와 분리한 비식별 로그로 보관(통계·VOC)하고, 탈퇴한 SNS 계정은 90일간 재가입이 제한된다.",
    )
    @DeleteMapping("api/v1/members/me")
    fun withdraw(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @Parameter(hidden = true)
        @BearerToken accessToken: String,
        @RequestBody @Valid request: WithdrawRequest,
    ): ResponseEntity<CommonResponse<Unit>>
}
