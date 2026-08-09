package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.DeleteMemberSajusPort
import com.yapp.todakun.shared.event.SajuChartChangedEvent
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

/**
 * 회원이 소유한 모든 사주 데이터를 삭제하는 크로스 도메인 유스케이스([DeleteMemberSajusPort] 구현).
 * 본인·상대 명식(자식 포함)과 소유권 링크를 함께 제거한다. 탈퇴 트랜잭션 안에서 호출된다.
 */
@CommandService
class DeleteMemberSajusService(
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
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

        // 명식 자체가 사라졌음을 알려 연도별 운세 캐시 등 파생 데이터를 정리하게 한다. AFTER_COMMIT
        // 리스너가 처리하므로 커밋 전 이벤트가 먼저 처리되는 경쟁이 생기지 않는다(이슈 #56).
        applicationEventPublisher.publishEvent(SajuChartChangedEvent(memberId))
    }
}
