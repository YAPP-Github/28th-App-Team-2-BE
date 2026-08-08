package com.yapp.todakun.config

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

private val log = LoggerFactory.getLogger(FailOpenCacheErrorHandler::class.java)

/**
 * Redis 장애가 API 장애로 번지지 않도록 캐시 조작 실패를 흡수한다(fail-open, 이슈 #56).
 * get 실패는 예외를 삼켜 캐시 미스로 처리되게 하여, `@Cacheable`이 붙은 원본 메서드가 DB 조회로
 * 정상 실행되도록 한다.
 */
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
        log.warn("캐시 무효화 실패. cache={}, key={}", cache.name, key, exception)
    }

    override fun handleCacheClearError(
        exception: RuntimeException,
        cache: Cache,
    ) {
        log.warn("캐시 전체 삭제 실패. cache={}", cache.name, exception)
    }
}
