package com.yapp.todakun.saju.application

import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.shared.event.SajuChartChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

/**
 * [SajuChartChangedEvent]는 `ReplaceSelfSajuChartService.replace()`/`DeleteMemberSajusService.deleteByMemberId()`
 * 안에서 발행되는데, 두 메서드 모두 그 자체가 최외곽 트랜잭션이 아니라 `UpdateMemberService`/
 * `WithdrawMemberTransactionService` 같은 바깥 `@CommandService` 안에서 호출된다(이슈 #56).
 * `@CacheEvict`를 이 메서드에 직접 붙이면 `RedisCacheConfig`의 `order`로 트랜잭션 어드바이저 바깥에
 * 캐싱을 둬도, REQUIRED 전파로 안쪽 트랜잭션 어드바이저가 물리 커밋 없이 반환한 직후 evict가 실행돼
 * 바깥 트랜잭션이 실제로 커밋되기 전에 캐시가 비워진다. 그 사이 동시 조회가 들어오면 옛 명식으로
 * 캐시가 재적립돼 SAJU_CHART_* TTL(30일) 동안 stale하게 남을 수 있다.
 * `AFTER_COMMIT` 리스너는 어느 트랜잭션이 최외곽이든 실제 물리 커밋 이후에만 실행되므로 이 문제가 없다.
 *
 * `cache.evict(...)`를 직접 호출하므로 `@CacheEvict` 어드바이저 경로에서만 동작하는
 * `RedisCacheConfig`의 fail-open `CacheErrorHandler`를 거치지 않는다. `AFTER_COMMIT` 리스너가
 * 예외를 던지면 이미 커밋된 호출자에게까지 예외가 전파될 수 있으므로, Redis 장애로 인한 evict 실패는
 * 여기서 직접 흡수한다.
 */
@Service
class SajuChartCacheEvictListener(
    private val cacheManager: CacheManager,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun evictSajuCharts(event: SajuChartChangedEvent) {
        evict(CacheNames.SAJU_CHART_DETAIL, event.memberId)
        evict(CacheNames.SAJU_CHART_SUMMARY, event.memberId)
    }

    private fun evict(
        cacheName: String,
        memberId: UUID,
    ) {
        val cache: Cache = cacheManager.getCache(cacheName) ?: return
        try {
            cache.evict(memberId)
        } catch (e: RuntimeException) {
            log.warn("사주 캐시 무효화 실패. cacheName={}, memberId={}", cacheName, memberId, e)
        }
    }
}

private val log = LoggerFactory.getLogger(SajuChartCacheEvictListener::class.java)
