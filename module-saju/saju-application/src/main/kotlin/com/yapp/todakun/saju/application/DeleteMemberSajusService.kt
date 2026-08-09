package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.common.transaction.runAfterCommit
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.DeleteMemberSajusPort
import com.yapp.todakun.shared.EvictYearSelectionFortunesPort
import org.springframework.cache.annotation.CacheEvict
import java.util.UUID

/**
 * 회원이 소유한 모든 사주 데이터를 삭제하는 크로스 도메인 유스케이스([DeleteMemberSajusPort] 구현).
 * 본인·상대 명식(자식 포함)과 소유권 링크를 함께 제거한다. 탈퇴 트랜잭션 안에서 호출된다.
 */
@CommandService
class DeleteMemberSajusService(
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val evictYearSelectionFortunesPort: EvictYearSelectionFortunesPort,
) : DeleteMemberSajusPort {
    // 탈퇴로 명식 자체가 사라지므로 GetMySajuService/GetSajuChartService 캐시도 함께 비운다(이슈 #56).
    @CacheEvict(cacheNames = [CacheNames.SAJU_CHART_DETAIL, CacheNames.SAJU_CHART_SUMMARY], key = "#memberId")
    override fun deleteByMemberId(memberId: UUID) {
        val links =
            buildList {
                memberSajuLinkRepository.findSelfByMemberId(memberId)?.let { add(it) }
                addAll(memberSajuLinkRepository.findPartnersByMemberId(memberId))
            }

        sajuChartRepository.deleteAllByIds(links.map { it.chartId })
        memberSajuLinkRepository.deleteByMemberId(memberId)

        // 명식 자체가 사라지므로 연도별 운세 캐시도 함께 비운다. 커밋 전에 비우면 그 사이 다른 트랜잭션이
        // 아직 삭제되지 않은 명식으로 캐시를 다시 채울 수 있어, 커밋 후에만 실행되도록 미룬다(이슈 #56).
        runAfterCommit { evictYearSelectionFortunesPort.evictByMemberId(memberId) }
    }
}
