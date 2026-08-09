package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
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
class EvictYearSelectionFortunesServiceTest : DescribeSpec({
    val yearSelectionFortuneRepository = mockk<YearSelectionFortuneRepository>()
    val cacheManager = mockk<CacheManager>()
    val cache = mockk<Cache>()
    val service = EvictYearSelectionFortunesService(yearSelectionFortuneRepository, cacheManager)

    val memberId = Uuid.generateV7().toJavaUuid()

    afterTest { clearMocks(yearSelectionFortuneRepository, cacheManager, cache) }

    describe("evictByMemberId") {
        context("회원이 생성한 연도별 운세가 있으면") {
            it("생성된 연도들의 캐시 키만 개별 evict한다") {
                every { yearSelectionFortuneRepository.findYearsByMemberId(memberId) } returns listOf(2025, 2026)
                every { cacheManager.getCache(CacheNames.YEAR_FORTUNE) } returns cache
                every { cache.evict(any()) } just Runs

                service.evictByMemberId(memberId)

                verify(exactly = 1) { cache.evict("$memberId:2025") }
                verify(exactly = 1) { cache.evict("$memberId:2026") }
            }
        }

        context("캐시가 아직 만들어지지 않았으면") {
            it("아무 것도 하지 않는다") {
                every { cacheManager.getCache(CacheNames.YEAR_FORTUNE) } returns null

                service.evictByMemberId(memberId)

                verify(exactly = 0) { yearSelectionFortuneRepository.findYearsByMemberId(any()) }
            }
        }
    }
})
