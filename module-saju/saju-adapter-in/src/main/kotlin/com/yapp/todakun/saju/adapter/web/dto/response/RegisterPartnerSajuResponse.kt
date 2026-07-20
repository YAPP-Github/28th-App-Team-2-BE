package com.yapp.todakun.saju.adapter.web.dto.response

import java.util.UUID

/** 상대방 사주 등록 결과. 생성된 소유권 링크 ID를 반환한다. */
data class RegisterPartnerSajuResponse(
    val linkId: UUID,
) {
    companion object {
        fun from(linkId: UUID): RegisterPartnerSajuResponse = RegisterPartnerSajuResponse(linkId)
    }
}
