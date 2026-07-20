package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.port.inbound.DeletePartnerSajuUseCase
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import java.util.UUID

/** 상대방 사주 삭제 유스케이스. 소유권 검증 후 명식(자식 포함)과 소유권 링크를 함께 제거한다. */
@CommandService
class DeletePartnerSajuService(
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
) : DeletePartnerSajuUseCase {
    override fun delete(
        memberId: UUID,
        linkId: UUID,
    ) {
        val link =
            memberSajuLinkRepository.findByIdAndMemberId(linkId, memberId)
                ?.takeIf { it.role == SajuRole.PARTNER }
                ?: throw SajuChartNotFoundException()

        sajuChartRepository.deleteById(link.chartId)
        memberSajuLinkRepository.deleteById(link.id)
    }
}
