package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.port.inbound.GetPartnerSajuUseCase
import com.yapp.todakun.saju.port.inbound.SajuChartDetail
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import java.util.UUID

/** 상대방 명식 상세 조회 유스케이스. 소유권을 검증하고 파생 상세(지장간·십이신살 포함)를 조립한다. */
@QueryService
class GetPartnerSajuService(
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val sajuChartRepository: SajuChartRepository,
) : GetPartnerSajuUseCase {
    override fun getPartner(
        memberId: UUID,
        linkId: UUID,
    ): SajuChartDetail {
        val link =
            memberSajuLinkRepository.findByIdAndMemberId(linkId, memberId)
                ?.takeIf { it.role == SajuRole.PARTNER }
                ?: throw SajuChartNotFoundException()
        val chart = sajuChartRepository.findById(link.chartId) ?: throw SajuChartNotFoundException()

        return SajuChartDetail.from(link, chart)
    }
}
