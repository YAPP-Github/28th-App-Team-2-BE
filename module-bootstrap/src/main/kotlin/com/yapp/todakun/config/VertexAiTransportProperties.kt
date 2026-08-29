package com.yapp.todakun.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Vertex AI gRPC/REST 전송 계층 자체의 RPC deadline(`vertex-ai.transport.*`).
 * [com.yapp.todakun.common.resilience.AiResilienceSupport]의 TimeLimiter는 응답 스레드를 취소할 뿐 이미 실행 중인 동기 Vertex AI 호출의 인터럽트를 보장하지 않는다.
 * 여기서 설정하는 값은 gRPC/HTTP 클라이언트가 실제로 네트워크 호출 자체를 중단시키는 전송 계층 타임아웃이라
 * TimeLimiter의 응답 여부와 무관하게 스레드 점유를 상한선 아래로 강제한다.
 *
 * 모든 도메인이 [org.springframework.ai.chat.client.ChatClient.Builder]가 감싸는 하나의 공유 [com.google.cloud.vertexai.VertexAI]
 * 클라이언트를 쓰므로(각 어댑터는 그 위에 자기 `ChatClient`만 새로 만든다) 이 값도 전역 하나다.
 * 그래서 가장 긴 사용처인 chat의 스트리밍(`STREAM_TIMEOUT` 60초)을 [streamTimeoutSeconds] 기본값의 기준으로 삼고,
 * 나머지 단건 호출 중 가장 긴 daily-fortune-ai의 TimeLimiter(60초)를 [unaryTimeoutSeconds] 기본값의 기준으로 삼아 각각 10초의 여유를 둔다.
 */
@ConfigurationProperties(prefix = "vertex-ai.transport")
data class VertexAiTransportProperties(
    val unaryTimeoutSeconds: Long = 70,
    val streamTimeoutSeconds: Long = 70,
)
