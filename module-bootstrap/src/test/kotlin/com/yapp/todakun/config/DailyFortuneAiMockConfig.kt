package com.yapp.todakun.config

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import org.springframework.boot.test.context.TestConfiguration

/**
 * VertexAiDailyFortuneAdapter는 ChatClient.Builder를 생성자에서 요구한다.
 * 테스트 컨텍스트는 spring.ai.model.chat=none으로 ChatModel 빈 생성을 막지만,
 * ChatClientAutoConfiguration은 별도 프로퍼티(spring.ai.chat.client.enabled)로 켜져 있어 ChatModel 빈 부재로 컨텍스트 로딩 자체가 실패한다.
 * DailyFortuneAiPort를 목으로 대체해 실제 구현체(VertexAiDailyFortuneAdapter) 생성 자체를 건너뛴다.
 * 이 포트를 실제로 호출하는 흐름(예: 회원가입)을 검증하는 테스트는 이 빈을 주입받아 직접 스텁한다.
 */
@TestConfiguration(proxyBeanMethods = false)
class DailyFortuneAiMockConfig {
    @MockkBean
    lateinit var dailyFortuneAiPort: DailyFortuneAiPort
}
