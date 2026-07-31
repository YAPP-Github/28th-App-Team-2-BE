package com.yapp.todakun.chat.adapter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

private const val CORE_POOL_SIZE = 4
private const val MAX_POOL_SIZE = 16
private const val QUEUE_CAPACITY = 100
private const val THREAD_NAME_PREFIX = "chat-stream-"

/** 답변 스트리밍(AI 호출 + SSE 전송)을 요청 스레드에서 분리해 실행하는 전용 워커 풀. */
@Configuration
class ChatStreamConfig {
    @Bean(CHAT_STREAM_EXECUTOR_BEAN_NAME)
    fun chatStreamTaskExecutor(): AsyncTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            setQueueCapacity(QUEUE_CAPACITY)
            setThreadNamePrefix(THREAD_NAME_PREFIX)
            initialize()
        }

    companion object {
        const val CHAT_STREAM_EXECUTOR_BEAN_NAME = "chatStreamTaskExecutor"
    }
}
