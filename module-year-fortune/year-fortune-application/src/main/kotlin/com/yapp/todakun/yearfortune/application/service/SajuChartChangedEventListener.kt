package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.shared.event.SajuChartChangedEvent
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 연도별 운세는 사주 명식을 기반으로 생성되므로, 사주가 바뀌거나 삭제되면 이미 캐시된 결과가 최대 90일
 * TTL 동안 stale하게 남는다(이슈 #56). [SajuChartChangedEvent]를 AFTER_COMMIT 단계에서만 처리해
 * 커밋 전 다른 트랜잭션이 아직 이전 명식으로 캐시를 다시 채우는 경쟁이나 롤백 시 불필요한 evict를 막는다.
 * 캐시 키가 `memberId:year`로 연도마다 달라 `@CacheEvict` 애노테이션만으로는 회원이 가진 모든 연도를
 * 한 번에 비울 수 없어, 실제 생성된 연도 목록을 조회한 뒤 그 키들만 개별 evict한다(다른 회원 캐시는
 * 건드리지 않기 위함).
 */
@Service
class SajuChartChangedEventListener(
    private val yearSelectionFortuneRepository: YearSelectionFortuneRepository,
    private val cacheManager: CacheManager,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun evictYearSelectionFortunes(event: SajuChartChangedEvent) {
        val cache = cacheManager.getCache(CacheNames.YEAR_FORTUNE) ?: return

        yearSelectionFortuneRepository.findYearsByMemberId(event.memberId).forEach { year ->
            cache.evict("${event.memberId}:$year")
        }
    }
}
