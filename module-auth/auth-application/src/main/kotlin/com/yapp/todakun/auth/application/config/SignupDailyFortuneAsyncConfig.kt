package com.yapp.todakun.auth.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

private const val CORE_POOL_SIZE = 2
private const val MAX_POOL_SIZE = 8
private const val QUEUE_CAPACITY = 50
private const val THREAD_NAME_PREFIX = "signup-fortune-"

/**
 * 회원가입 직후 당일 운세 생성(AI 호출)을 요청 스레드에서 분리해 실행하는 전용 워커 풀.
 * HikariCP 커넥션 풀과 무관하게 별도 산정한다.
 * AI 호출은 DB 커넥션을 점유하지 않으므로 풀 크기를 커넥션 풀 크기에 맞출 이유가 없다.
 */
@Configuration
class SignupDailyFortuneAsyncConfig {
    @Bean(SIGNUP_DAILY_FORTUNE_EXECUTOR_BEAN_NAME)
    fun signupDailyFortuneTaskExecutor(): AsyncTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            setQueueCapacity(QUEUE_CAPACITY)
            setThreadNamePrefix(THREAD_NAME_PREFIX)
            initialize()
        }

    companion object {
        const val SIGNUP_DAILY_FORTUNE_EXECUTOR_BEAN_NAME = "signupDailyFortuneTaskExecutor"
    }
}
