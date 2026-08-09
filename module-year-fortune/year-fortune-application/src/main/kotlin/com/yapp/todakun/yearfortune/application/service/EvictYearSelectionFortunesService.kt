package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.shared.EvictYearSelectionFortunesPort
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 연도별 운세는 사주 명식을 기반으로 생성되므로, 사주가 바뀌거나 삭제되면 이미 캐시된 결과가 최대 90일
 * TTL 동안 stale하게 남는다(이슈 #56). 캐시 키가 `memberId:year`로 연도마다 달라 `@CacheEvict`
 * 애노테이션만으로는 회원이 가진 모든 연도를 한 번에 비울 수 없어, 실제 생성된 연도 목록을 조회한 뒤
 * 그 키들만 개별 evict한다(회원 전체 캐시만 지우고 다른 회원 캐시는 건드리지 않기 위함).
 */
@Service
class EvictYearSelectionFortunesService(
    private val yearSelectionFortuneRepository: YearSelectionFortuneRepository,
    private val cacheManager: CacheManager,
) : EvictYearSelectionFortunesPort {
    override fun evictByMemberId(memberId: UUID) {
        val cache = cacheManager.getCache(CacheNames.YEAR_FORTUNE) ?: return

        yearSelectionFortuneRepository.findYearsByMemberId(memberId).forEach { year ->
            cache.evict("$memberId:$year")
        }
    }
}
