package com.yapp.todakun.common.cache

import java.time.Duration

/**
 * 캐시 이름과 TTL 정책(이슈 #56).
 *
 * `*-application` 모듈의 `@Cacheable`/`@CacheEvict`와 bootstrap의 `RedisCacheConfig`가 같은 이름을
 * 참조해야 해 두 계층 어디에도 속하지 않는 `:common`에 둔다. Redis 타입에는 의존하지 않는 순수
 * 이름·TTL 정책만 가진다(RedisCacheConfiguration 매핑은 bootstrap의 몫).
 *
 * TODAY_FORTUNE·LUCK_ACTIONS는 캐시 키에 조회일(fortuneDate)이 포함되어 날짜가 바뀌면 키 자체가 달라지므로,
 * 엔트리 TTL 24시간은 "자정 만료"를 흉내내는 값이 아니라 더는 조회되지 않는 이전 날짜 키를 자연 소멸시키는 안전장치다.
 * SAJU_CHART_DETAIL·SAJU_CHART_SUMMARY·TERMS는 값이 거의 바뀌지 않아 장기 TTL과 명시적 evict가 무효화를 주도하고, TTL은 evict 누락에 대비한 안전장치로만 둔다.
 * SAJU_CHART_DETAIL(GetMySajuUseCase)과 SAJU_CHART_SUMMARY(GetSajuChartPort)는 같은 memberId를 키로 쓰지만 반환 타입이 다른 별개 조회라 캐시 이름을 분리한다.
 * 하나로 합치면 같은 `{cacheName}:{memberId}` 키에 서로 다른 타입이 덮어써진다.
 * YEAR_FORTUNE은 `CreateYearSelectionFortuneService.create()`(get-or-create)에 그대로 `@Cacheable`을 붙인다.
 * 캐시 히트 시 락·DB 조회 없이 즉시 반환되고, 캐시 미스일 때만 기존 락 기반 생성 로직이 그대로 실행된다.
 * 단, 이 메서드는 트랜잭션 안에서 쓰기(save)까지 하므로 캐시 적립이 커밋 *이전*에 일어나면 롤백 시 존재하지 않는 데이터가 영구 캐싱될 수 있어,
 * RedisCacheConfig에서 캐싱 어드바이저가 트랜잭션 어드바이저보다 바깥에서 실행되도록 순서를 명시적으로 고정했다. AI 재호출 비용이 커서 TTL을 SAJU_CHART보다 길게 잡는다.
 */
object CacheNames {
    const val TODAY_FORTUNE = "today-fortune"
    const val LUCK_ACTIONS = "luck-actions"
    const val SAJU_CHART_DETAIL = "saju-chart-detail"
    const val SAJU_CHART_SUMMARY = "saju-chart-summary"
    const val TERMS = "terms"
    const val YEAR_FORTUNE = "year-fortune"

    val ttlByCacheName: Map<String, Duration> =
        mapOf(
            TODAY_FORTUNE to Duration.ofHours(24),
            LUCK_ACTIONS to Duration.ofHours(24),
            SAJU_CHART_DETAIL to Duration.ofDays(30),
            SAJU_CHART_SUMMARY to Duration.ofDays(30),
            TERMS to Duration.ofDays(7),
            YEAR_FORTUNE to Duration.ofDays(90),
        )
}
