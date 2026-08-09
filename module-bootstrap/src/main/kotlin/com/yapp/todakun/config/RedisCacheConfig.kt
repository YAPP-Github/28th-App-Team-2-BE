package com.yapp.todakun.config

import com.yapp.todakun.common.cache.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

/**
 * 조회 API 응답 캐싱 인프라(이슈 #56).
 *
 * 캐시 대상이 daily-fortune·saju·terms·luck 등 여러 도메인에 걸쳐 있어 특정 도메인의 adapter-out에 두면 다른 도메인이 그 모듈에 의존해야 하는 역방향 결합이 생긴다.
 * [SecurityConfig] 등 다른 횡단 관심사 설정과 동일하게, 도메인 어디에도 속하지 않는 합성 루트(bootstrap)에 둔다.
 * application 계층은 `@Cacheable`/`@CacheEvict` 표준 애너테이션만 사용하므로 "application은 adapter 패키지를 참조하지 않는다"는 Konsist 규칙과 충돌하지 않는다.
 *
 * 키 prefix는 프로젝트 컨벤션(`chat:quota:{memberId}:{yyyyMMdd}` 등)에 맞춰 기본 `::` 구분자 대신 단일 `:`를 쓰도록 computePrefixWith를 재정의한다.
 * 값 직렬화는 역직렬화 시 구체 타입을 복원해야 하므로 다형 타입 정보를 함께 저장하는 [GenericJacksonJsonRedisSerializer]를 사용하되,
 * 임의 클래스 역직렬화(가젯 공격)를 막기 위해 [BasicPolymorphicTypeValidator]로 허용 패키지를 `com.yapp.todakun` 하위로 제한한다.
 *
 * `order`: `ReplaceSelfSajuChartService.replace()`/`DeleteMemberSajusService.deleteByMemberId()`처럼
 * `@CommandService`(=`@Transactional`) 메서드에 `@CacheEvict`를 붙이는 경우,
 * 캐싱 어드바이저가 트랜잭션 어드바이저보다 안쪽에서 실행되면 커밋 전에 무효화가 일어나 롤백 시에도 여전히 유효한 캐시를 불필요하게 비우게 된다.
 * 트랜잭션 기본 순서([Ordered.LOWEST_PRECEDENCE])보다 한 단계 앞세워 캐싱이 항상 트랜잭션 바깥(커밋 이후)에서 동작하도록 고정한다.
 * 읽기 전용 캐시(today-fortune 등)에는 영향이 없다.
 *
 * `cacheManager()`에 `@Bean`을 붙여 [CachingConfigurer]가 내부적으로 쓰는 것과는 별개로 컨테이너에 `CacheManager` 빈으로도 등록한다.
 *  `@Bean` 없이 override만 하면 캐싱 AOP 프록시는 정상 동작하지만 다른 컴포넌트(테스트 등)가 `CacheManager`를 주입받을 수 없다.
 */
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
class RedisCacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
) : CachingConfigurer {
    @Bean
    override fun cacheManager(): CacheManager {
        val defaultConfig = defaultCacheConfiguration()
        val ttlConfigurations = CacheNames.ttlByCacheName.mapValues { (_, ttl) -> defaultConfig.entryTtl(ttl) }

        // LettuceConnectionFactory는 ReactiveRedisConnectionFactory이기도 해서,
        // RedisCacheManager.builder(connectionFactory)가 내부적으로 만드는 기본 RedisCacheWriter는 put/evict를 비동기(fire-and-forget)로 처리한다
        // Redis ACK을 기다리지 않고 다음 코드로 진행되어, 바로 이어지는 조회가 방금 쓴 값을 못 보고 캐시 미스로 리포지토리를 다시 타는 레이스가 생긴다.
        // immediateWrites()로 put/evict가 ACK까지 블로킹하도록 강제해 read-after-write 일관성을 보장한다.
        val cacheWriter = RedisCacheWriter.create(redisConnectionFactory) { it.immediateWrites() }

        return RedisCacheManager.builder(cacheWriter)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(ttlConfigurations)
            .build()
    }

    override fun errorHandler(): CacheErrorHandler = FailOpenCacheErrorHandler

    private fun defaultCacheConfiguration(): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .computePrefixWith { "$it:" }
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()))

    private fun valueSerializer(): GenericJacksonJsonRedisSerializer =
        GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(typeValidator())
            .customize { it.findAndAddModules() }
            .build()

    private fun typeValidator(): BasicPolymorphicTypeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.yapp.todakun.")
            .allowIfSubType("java.util.")
            .allowIfSubType("kotlin.collections.")
            .build()
}
