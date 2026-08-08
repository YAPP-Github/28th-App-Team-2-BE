package com.yapp.todakun.common.cache

import java.time.Duration

/**
 * 캐시 이름과 TTL 정책(이슈 #56).
 *
 * `*-application` 모듈의 `@Cacheable`/`@CacheEvict`와 bootstrap의 `RedisCacheConfig`가 같은 이름을
 * 참조해야 해 두 계층 어디에도 속하지 않는 `:common`에 둔다. Redis 타입에는 의존하지 않는 순수
 * 이름·TTL 정책만 가진다(RedisCacheConfiguration 매핑은 bootstrap의 몫).
 *
 * TODAY_FORTUNE·LUCK_ACTIONS는 캐시 키에 조회일(fortuneDate)이 포함되어 날짜가 바뀌면 키 자체가
 * 달라지므로, 엔트리 TTL 24시간은 "자정 만료"를 흉내내는 값이 아니라 더는 조회되지 않는 이전 날짜
 * 키를 자연 소멸시키는 안전장치다.
 * SAJU_CHART·TERMS는 값이 거의 바뀌지 않아 장기 TTL과 명시적 evict가 무효화를 주도하고, TTL은
 * evict 누락에 대비한 안전장치로만 둔다.
 * YEAR_FORTUNE은 조회와 생성을 겸하는 멱등 서비스라 캐싱 전략을 별도로 설계해야 해 제외한다.
 */
object CacheNames {
    const val TODAY_FORTUNE = "today-fortune"
    const val LUCK_ACTIONS = "luck-actions"
    const val SAJU_CHART = "saju-chart"
    const val TERMS = "terms"

    val ttlByCacheName: Map<String, Duration> =
        mapOf(
            TODAY_FORTUNE to Duration.ofHours(24),
            LUCK_ACTIONS to Duration.ofHours(24),
            SAJU_CHART to Duration.ofDays(30),
            TERMS to Duration.ofDays(7),
        )
}
