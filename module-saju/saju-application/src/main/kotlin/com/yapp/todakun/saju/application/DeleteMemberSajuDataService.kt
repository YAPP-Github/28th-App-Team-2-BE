package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.DeleteMemberSajuDataPort
import java.util.UUID

/**
 * 회원이 소유한 모든 사주 데이터를 삭제하는 크로스 도메인 유스케이스([DeleteMemberSajuDataPort] 구현).
 * 본인·상대 명식(자식 포함)과 소유권 링크를 함께 제거한다. 탈퇴 트랜잭션 안에서 호출된다.
 */
@CommandService
class DeleteMemberSajuDataService(
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
) : DeleteMemberSajuDataPort {
    override fun deleteByMemberId(memberId: UUID) {
        val links =
            buildList {
                memberSajuLinkRepository.findSelfByMemberId(memberId)?.let { add(it) }
                addAll(memberSajuLinkRepository.findPartnersByMemberId(memberId))
            }

        sajuChartRepository.deleteAllByIds(links.map { it.chartId })
        memberSajuLinkRepository.deleteByMemberId(memberId)
    }
}
