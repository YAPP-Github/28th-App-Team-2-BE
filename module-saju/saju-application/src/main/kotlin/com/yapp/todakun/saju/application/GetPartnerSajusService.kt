package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.port.inbound.GetPartnerSajusUseCase
import com.yapp.todakun.saju.port.inbound.PartnerSajuSummary
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import java.util.UUID

/** 회원이 등록한 상대방 사주 목록 조회 유스케이스(등록 순, 전체 반환). */
@QueryService
class GetPartnerSajusService(
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val sajuChartRepository: SajuChartRepository,
) : GetPartnerSajusUseCase {
    override fun getPartners(memberId: UUID): List<PartnerSajuSummary> {
        val links = memberSajuLinkRepository.findPartnersByMemberId(memberId)
        val summariesByChartId = sajuChartRepository.findSummariesByIds(links.map { it.chartId }).associateBy { it.id }
        return links.map { link ->
            val summary = summariesByChartId[link.chartId] ?: throw SajuChartNotFoundException()
            PartnerSajuSummary.from(link, summary)
        }
    }
}
