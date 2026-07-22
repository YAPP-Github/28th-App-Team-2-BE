package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.port.inbound.GetMySajuUseCase
import com.yapp.todakun.saju.port.inbound.SajuChartDetail
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import java.util.UUID

/** 회원 본인(SELF) 만세력 상세 조회 유스케이스. 회원가입 때 저장된 SELF 명식을 읽어 파생 상세를 조립한다. */
@QueryService
class GetMySajuService(
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val sajuChartRepository: SajuChartRepository,
) : GetMySajuUseCase {
    override fun getMine(memberId: UUID): SajuChartDetail {
        val link = memberSajuLinkRepository.findSelfByMemberId(memberId) ?: throw SajuChartNotFoundException()
        val chart = sajuChartRepository.findById(link.chartId) ?: throw SajuChartNotFoundException()

        return SajuChartDetail.from(link, chart)
    }
}
