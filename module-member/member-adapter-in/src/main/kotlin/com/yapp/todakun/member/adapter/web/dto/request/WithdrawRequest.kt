package com.yapp.todakun.member.adapter.web.dto.request

import com.yapp.todakun.member.WithdrawalReason
import com.yapp.todakun.member.adapter.web.dto.toMemberEnum
import com.yapp.todakun.member.port.inbound.WithdrawCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

/** 회원 탈퇴 요청. [detail]은 사유가 기타(ETC)일 때 입력하는 상세(선택, 최대 500자). */
data class WithdrawRequest(
    @field:Schema(description = "탈퇴 사유", example = "NOT_USING")
    @field:NotBlank(message = "탈퇴 사유를 선택해 주세요.")
    @field:Pattern(
        regexp = "^(PRIVACY_CONCERN|TOO_MANY_NOTIFICATIONS|LACK_OF_CONTENT|NOT_USING|ETC)$",
        message = "올바른 탈퇴 사유 값이 아닙니다.",
    )
    val reason: String,
    @field:Schema(description = "탈퇴 사유 상세(선택)", example = "자주 들어오지 않게 되었어요.")
    @field:Size(max = 500, message = "상세 사유는 최대 500자까지 입력할 수 있습니다.")
    val detail: String? = null,
) {
    fun toCommand(memberId: UUID) =
        WithdrawCommand(
            memberId = memberId,
            reason = reason.toMemberEnum<WithdrawalReason>(),
            detail = detail?.takeIf { it.isNotBlank() },
        )
}
