package com.yapp.todakun.saju.application

import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.shared.event.SajuChartChangedEvent
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class SajuChartCacheEvictListenerTest : DescribeSpec({
    val cacheManager = mockk<CacheManager>()
    val detailCache = mockk<Cache>()
    val summaryCache = mockk<Cache>()
    val listener = SajuChartCacheEvictListener(cacheManager)

    val memberId = Uuid.generateV7().toJavaUuid()

    afterTest { clearMocks(cacheManager, detailCache, summaryCache) }

    describe("evictSajuCharts") {
        context("두 캐시가 모두 만들어져 있으면") {
            it("SAJU_CHART_DETAIL/SAJU_CHART_SUMMARY 둘 다 회원 키로 evict한다") {
                every { cacheManager.getCache(CacheNames.SAJU_CHART_DETAIL) } returns detailCache
                every { cacheManager.getCache(CacheNames.SAJU_CHART_SUMMARY) } returns summaryCache
                every { detailCache.evict(memberId) } just Runs
                every { summaryCache.evict(memberId) } just Runs

                listener.evictSajuCharts(SajuChartChangedEvent(memberId))

                verify(exactly = 1) { detailCache.evict(memberId) }
                verify(exactly = 1) { summaryCache.evict(memberId) }
            }
        }

        context("캐시가 아직 만들어지지 않았으면") {
            it("아무 것도 하지 않는다") {
                every { cacheManager.getCache(CacheNames.SAJU_CHART_DETAIL) } returns null
                every { cacheManager.getCache(CacheNames.SAJU_CHART_SUMMARY) } returns null

                listener.evictSajuCharts(SajuChartChangedEvent(memberId))

                verify(exactly = 0) { detailCache.evict(any()) }
                verify(exactly = 0) { summaryCache.evict(any()) }
            }
        }

        context("Redis 장애로 evict가 실패하면") {
            it("예외를 전파하지 않고 나머지 캐시의 evict를 계속 시도한다") {
                every { cacheManager.getCache(CacheNames.SAJU_CHART_DETAIL) } returns detailCache
                every { cacheManager.getCache(CacheNames.SAJU_CHART_SUMMARY) } returns summaryCache
                every { detailCache.evict(memberId) } throws RuntimeException("Redis 연결 실패")
                every { summaryCache.evict(memberId) } just Runs

                listener.evictSajuCharts(SajuChartChangedEvent(memberId))

                verify(exactly = 1) { summaryCache.evict(memberId) }
            }
        }
    }
})
