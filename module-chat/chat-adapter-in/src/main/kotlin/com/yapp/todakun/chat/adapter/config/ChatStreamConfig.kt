package com.yapp.todakun.chat.adapter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

private const val CORE_POOL_SIZE = 8
private const val MAX_POOL_SIZE = 16
private const val QUEUE_CAPACITY = 30
private const val THREAD_NAME_PREFIX = "chat-stream-"
private const val AWAIT_TERMINATION_SECONDS = 30

/**
 * 답변 스트리밍(AI 호출 + SSE 전송)을 요청 스레드에서 분리해 실행하는 전용 워커 풀.
 *
 * [ThreadPoolExecutor][java.util.concurrent.ThreadPoolExecutor]는 core가 다 차야 큐잉하고, 큐까지 가득 차야 core 이상으로 스레드를 늘린다.
 * 큐를 크게 두면 대기 작업이 그만큼 쌓이기 전에는 실효 동시성이 [CORE_POOL_SIZE]에 고정돼 [MAX_POOL_SIZE]까지 늘 일이 없으므로,
 * core를 평상시 목표 동시성으로 두고 큐는 순간 버스트를 흡수할 정도로만 작게 둔다(chat-ai는 TimeLimiter를 쓰지 않아
 * [AiResilienceConfig][com.yapp.todakun.config.AiResilienceConfig]의 격리 executor를 거치지 않으므로 daily-fortune과 달리 하류 공유 예산 제약은 없다).
 */
@Configuration
class ChatStreamConfig {
    @Bean(CHAT_STREAM_EXECUTOR_BEAN_NAME)
    fun chatStreamTaskExecutor(): AsyncTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            setQueueCapacity(QUEUE_CAPACITY)
            setThreadNamePrefix(THREAD_NAME_PREFIX)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS)
            initialize()
        }

    companion object {
        const val CHAT_STREAM_EXECUTOR_BEAN_NAME = "chatStreamTaskExecutor"
    }
}
