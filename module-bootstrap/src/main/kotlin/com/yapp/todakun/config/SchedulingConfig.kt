package com.yapp.todakun.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * @Scheduled 스케줄러(알림 발송 등)를 활성화한다.
 * `scheduling.enabled=false`로 끌 수 있다 — 알림 재시도 폴러가 매분 도는 탓에 `@SpringBootTest` 통합 테스트
 * 컨텍스트가 떠 있는 동안(특히 Testcontainers가 이미 정리된 종료 시점) 스케줄러가 실행돼 로그 노이즈·커넥션 풀 경합이
 * 생길 수 있어, 테스트 프로필(`application.yaml`)에서 꺼둔다.
 */
@Configuration
@ConditionalOnProperty(prefix = "scheduling", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableScheduling
class SchedulingConfig
