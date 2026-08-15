package com.yapp.todakun.config

import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.PredictionServiceClient
import com.google.cloud.vertexai.api.PredictionServiceSettings
import org.springframework.ai.model.SpringAIModelProperties
import org.springframework.ai.model.SpringAIModels
import org.springframework.ai.model.vertexai.autoconfigure.gemini.VertexAiGeminiConnectionProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.Assert
import java.time.Duration
import com.google.cloud.vertexai.Transport as VertexTransport

/**
 * spring-ai의 `VertexAiGeminiChatAutoConfiguration`이 만드는 기본 [VertexAI] 빈은 RPC deadline을 설정할 방법을 노출하지 않는다.
 * 그래서 [VertexAiGeminiConnectionProperties]를 그대로 읽어 이 클래스가 직접 [VertexAI] 빈을 만들고, `@ConditionalOnMissingBean`인 autoconfig의 기본 빈을 대신한다.
 *
 * `spring.ai.model.chat=none`이면(bootstrap 테스트 컨텍스트가 실제 Vertex AI 자격 증명 없이 기동하기 위해 사용한다,
 * `DailyFortuneAiMockConfig` 참고) autoconfig 원본과 동일하게 이 빈도 만들지 않는다 — 그렇지 않으면 project-id가 없는 테스트 환경에서 이 빈이 무조건 즉시 생성되며 기동에 실패한다.
 *
 * 이 프로젝트는 spring.ai.vertex.ai.gemini.scopes/credentials-uri를 쓰지 않으므로(ADC 인증만 사용),
 * 두 값이 설정되면 이 커스텀 구성이 조용히 무시하지 않고 기동 시점에 바로 실패하게 한다.
 */
@Configuration
@ConditionalOnProperty(name = [SpringAIModelProperties.CHAT_MODEL], havingValue = SpringAIModels.VERTEX_AI, matchIfMissing = true)
@EnableConfigurationProperties(
    VertexAiTransportProperties::class,
    // 이 클래스가 직접 의존하는 프로퍼티라 자체 등록한다(spring-ai의 VertexAiGeminiChatAutoConfiguration도 같은 조건에서 등록하지만, 그 구현 세부사항에 기대지 않기 위함).
    VertexAiGeminiConnectionProperties::class,
)
class VertexAiTransportConfig {
    @Bean
    fun vertexAi(
        connectionProperties: VertexAiGeminiConnectionProperties,
        transportProperties: VertexAiTransportProperties,
    ): VertexAI {
        Assert.hasText(connectionProperties.projectId, "Vertex AI project-id must be set!")
        Assert.hasText(connectionProperties.location, "Vertex AI location must be set!")
        Assert.isTrue(
            connectionProperties.scopes.isEmpty(),
            "VertexAiTransportConfig는 spring.ai.vertex.ai.gemini.scopes를 지원하지 않습니다.",
        )
        Assert.isTrue(
            connectionProperties.credentialsUri == null,
            "VertexAiTransportConfig는 spring.ai.vertex.ai.gemini.credentials-uri를 지원하지 않습니다.",
        )

        val apiEndpoint =
            connectionProperties.apiEndpoint?.takeIf { it.isNotBlank() }
                ?: "${connectionProperties.location}-aiplatform.googleapis.com"

        val settingsBuilder =
            if (connectionProperties.transport == VertexAiGeminiConnectionProperties.Transport.REST) {
                PredictionServiceSettings.newHttpJsonBuilder()
            } else {
                PredictionServiceSettings.newBuilder()
            }
        settingsBuilder.setEndpoint("$apiEndpoint:443")
        settingsBuilder.setCredentialsProvider(PredictionServiceSettings.defaultCredentialsProviderBuilder().build())
        settingsBuilder.setHeaderProvider(PredictionServiceSettings.defaultApiClientHeaderProviderBuilder().build())
        // 스트리밍이 아닌 단건 호출(generate) — TimeLimiter가 적용된 도메인들이 여기 해당한다.
        settingsBuilder
            .generateContentSettings()
            .setSimpleTimeoutNoRetriesDuration(Duration.ofSeconds(transportProperties.unaryTimeoutSeconds))
        // 스트리밍 호출 — chat의 토큰 스트리밍(streamAnswer)이 여기 해당하며 unary보다 오래 걸린다.
        settingsBuilder
            .streamGenerateContentSettings()
            .setSimpleTimeoutNoRetriesDuration(Duration.ofSeconds(transportProperties.streamTimeoutSeconds))

        // PredictionServiceClient.create()가 ADC 자격 증명을 즉시 확인하므로, 기존 spring-ai 기본 구현과 동일하게
        // 첫 API 호출 시점까지 생성을 미룬다(그렇지 않으면 로컬/테스트 기동이 ADC 설정 여부에 즉시 좌우된다).
        val predictionServiceClient by lazy { PredictionServiceClient.create(settingsBuilder.build()) }

        val vertexAiBuilder =
            VertexAI.Builder()
                .setProjectId(connectionProperties.projectId)
                .setLocation(connectionProperties.location)
                .setTransport(VertexTransport.valueOf(connectionProperties.transport.name))
                .setPredictionClientSupplier { predictionServiceClient }
        if (!connectionProperties.apiEndpoint.isNullOrBlank()) {
            vertexAiBuilder.setApiEndpoint(connectionProperties.apiEndpoint)
        }

        return vertexAiBuilder.build()
    }
}
