package com.yapp.todakun.config

import com.yapp.todakun.common.logging.Loggable
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

/**
 * Redis 장애가 API 장애로 번지지 않도록 캐시 조작 실패를 흡수한다(fail-open, 이슈 #56).
 * get 실패는 예외를 삼켜 캐시 미스로 처리되게 하여, `@Cacheable`이 붙은 원본 메서드가 DB 조회로
 * 정상 실행되도록 한다.
 *
 * evict 실패는 get/put 실패와 성격이 다르다. get/put은 실패해도 다음 조회가 DB에서 다시 읽어와 정합성이 유지되지만,
 * evict 실패는 이미 바뀐 데이터가 옛 캐시로 남아 TTL(SAJU_CHART_* 30일, YEAR_FORTUNE 90일) 동안 조용히 stale 응답을 준다.
 * 이는 로그만 남겨서는 아무도 모르고 지나치는 사건이라 ERROR로 올려
 * 기존 Discord ERROR 웹훅(Grafana Cloud, `error-discord-alert.yaml`) 알림이 걸리게 한다.
 */
@Loggable
object FailOpenCacheErrorHandler : CacheErrorHandler {
    override fun handleCacheGetError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        log.warn("캐시 조회 실패, DB 조회로 폴백합니다. cache={}, key={}", cache.name, key, exception)
    }

    override fun handleCachePutError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
        value: Any?,
    ) {
        log.warn("캐시 저장 실패. cache={}, key={}", cache.name, key, exception)
    }

    override fun handleCacheEvictError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        log.error("캐시 무효화 실패. cache={}, key={}", cache.name, key, exception)
    }

    override fun handleCacheClearError(
        exception: RuntimeException,
        cache: Cache,
    ) {
        log.warn("캐시 전체 삭제 실패. cache={}", cache.name, exception)
    }
}
