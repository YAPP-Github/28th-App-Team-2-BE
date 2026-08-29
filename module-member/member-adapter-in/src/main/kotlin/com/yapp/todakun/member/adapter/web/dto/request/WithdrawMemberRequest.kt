package com.yapp.todakun.member.adapter.web.dto.request

import com.yapp.todakun.member.WithdrawalReason
import com.yapp.todakun.member.adapter.web.dto.toMemberEnum
import com.yapp.todakun.member.port.inbound.WithdrawMemberCommand
import com.yapp.todakun.web.validation.ValidEnum
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * 회원 탈퇴 요청. [reason]은 필수(단일 선택, 9종). [detail]은 상세 사유(최대 200자)로,
 * 사유가 기타(ETC)이면 필수이고 그 외에는 선택이다(탈퇴 정책 v1.0 — 6장).
 */
data class WithdrawMemberRequest(
    @field:Schema(
        description = "탈퇴 사유(단일 선택)",
        example = "LOW_USAGE",
        allowableValues = [
            "CONTENT_INAPPROPRIATE",
            "CHATBOT_UNSATISFACTORY",
            "LOW_USAGE",
            "MISSING_FEATURE",
            "PAYMENT_INCONVENIENCE",
            "PRIVACY_CONCERN",
            "FREQUENT_ERRORS",
            "SWITCHING_SERVICE",
            "ETC",
        ],
    )
    @field:NotBlank(message = "탈퇴 사유를 선택해 주세요.")
    @field:ValidEnum(enumClass = WithdrawalReason::class, message = "올바른 탈퇴 사유 값이 아닙니다.")
    val reason: String,
    @field:Schema(description = "탈퇴 사유 상세. '기타(ETC)' 선택 시 필수, 그 외 선택(최대 200자)", example = "자주 들어오지 않게 되었어요.")
    @field:Size(max = 200, message = "상세 사유는 최대 200자까지 입력할 수 있습니다.")
    val detail: String? = null,
) {
    /** '기타(ETC)' 선택 시 상세 사유는 공백 없이 입력해야 한다(탈퇴 정책 6-3). */
    @get:AssertTrue(message = "기타 사유를 선택한 경우 상세 사유를 입력해 주세요.")
    @get:Schema(hidden = true)
    val isEtcDetailProvided: Boolean
        get() = reason != WithdrawalReason.ETC.name || !detail.isNullOrBlank()

    fun toCommand(
        memberId: UUID,
        accessToken: String,
    ) = WithdrawMemberCommand(
        memberId = memberId,
        reason = reason.toMemberEnum<WithdrawalReason>(),
        detail = detail?.takeIf { it.isNotBlank() },
        accessToken = accessToken,
    )
}
