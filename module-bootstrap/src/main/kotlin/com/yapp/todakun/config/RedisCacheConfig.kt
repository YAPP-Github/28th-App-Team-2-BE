package com.yapp.todakun.config

import com.yapp.todakun.common.cache.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

/**
 * 조회 API 응답 캐싱 인프라(이슈 #56).
 *
 * 캐시 대상이 daily-fortune·saju·terms·luck 등 여러 도메인에 걸쳐 있어 특정 도메인의 adapter-out에
 * 두면 다른 도메인이 그 모듈에 의존해야 하는 역방향 결합이 생긴다. [SecurityConfig] 등 다른 횡단
 * 관심사 설정과 동일하게, 도메인 어디에도 속하지 않는 합성 루트(bootstrap)에 둔다. application
 * 계층은 `@Cacheable`/`@CacheEvict` 표준 애너테이션만 사용하므로 "application은 adapter 패키지를
 * 참조하지 않는다"는 Konsist 규칙과 충돌하지 않는다.
 *
 * 키 prefix는 프로젝트 컨벤션(`chat:quota:{memberId}:{yyyyMMdd}` 등)에 맞춰 기본 `::` 구분자
 * 대신 단일 `:`를 쓰도록 [computePrefixWith]를 재정의한다. 값 직렬화는 역직렬화 시 구체 타입을
 * 복원해야 하므로 다형 타입 정보를 함께 저장하는 [GenericJacksonJsonRedisSerializer]를 사용하되,
 * 임의 클래스 역직렬화(가젯 공격)를 막기 위해 [BasicPolymorphicTypeValidator]로 허용 패키지를
 * `com.yapp.todakun` 하위로 제한한다.
 */
@Configuration
@EnableCaching
class RedisCacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
) : CachingConfigurer {
    override fun cacheManager(): CacheManager {
        val defaultConfig = defaultCacheConfiguration()
        val ttlConfigurations = CacheNames.ttlByCacheName.mapValues { (_, ttl) -> defaultConfig.entryTtl(ttl) }

        return RedisCacheManager.builder(redisConnectionFactory)
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
