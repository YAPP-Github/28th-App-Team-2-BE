package com.yapp.todakun.compatibility.adapter.web

import com.yapp.todakun.compatibility.adapter.web.dto.response.CompatibilityResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID

@Tag(name = "Compatibility", description = "상대방 궁합 API")
interface CompatibilityApi {
    @Operation(
        summary = "상대방 궁합 생성",
        description =
            "회원 본인(SELF) 명식과 선택한 상대(저장된 상대 사주) 명식을 조합해 궁합을 생성한다. " +
                "이미 생성된 조합이면 기존 결과를 반환한다(멱등). 사주원국은 응답에 포함하지 않으며 saju 상세 API로 별도 조회한다.",
    )
    @PostMapping("api/v1/compatibilities/{partnerLinkId}")
    fun create(
        @Parameter(description = "궁합 상대(저장된 상대 사주) 링크 ID", example = "018f0000-0000-7000-8000-000000000003")
        @PathVariable partnerLinkId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<CompatibilityResponse>>

    @Operation(
        summary = "궁합 단건 조회",
        description = "생성된 궁합을 ID로 단건 조회한다. 공유하기 화면에서 사용한다. 사주원국은 응답에 포함하지 않으며 saju 상세 API로 별도 조회한다.",
    )
    @GetMapping("api/v1/compatibilities/{compatibilityId}")
    fun getById(
        @Parameter(description = "궁합 ID", example = "018f0000-0000-7000-8000-0000000000e1")
        @PathVariable compatibilityId: UUID,
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<CompatibilityResponse>>
}
