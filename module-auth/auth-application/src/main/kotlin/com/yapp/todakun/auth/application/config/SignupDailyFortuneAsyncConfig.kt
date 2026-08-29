package com.yapp.todakun.auth.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

private const val CORE_POOL_SIZE = 6
private const val MAX_POOL_SIZE = 8
private const val QUEUE_CAPACITY = 20
private const val THREAD_NAME_PREFIX = "signup-fortune-"

// TimeLimiter(60s)는 실행 중인 동기 호출의 스레드 인터럽트를 보장하지 않으므로,
// 실제 스레드 점유 상한은 `vertex-ai.transport.unary-timeout-seconds`(70s)다. 그보다 짧으면
// 종료 시점에 정상 완료 직전인 작업까지 끊길 수 있어 70s + 여유로 잡는다.
private const val AWAIT_TERMINATION_SECONDS = 80

/**
 * 회원가입 직후 당일 운세 생성(AI 호출)을 요청 스레드에서 분리해 실행하는 전용 워커 풀.
 * HikariCP 커넥션 풀과 무관하게 별도 산정한다. AI 호출은 DB 커넥션을 점유하지 않으므로 풀 크기를 커넥션 풀 크기에 맞출 이유가 없다.
 *
 * [ThreadPoolExecutor][java.util.concurrent.ThreadPoolExecutor]는 core가 다 차야 큐잉하고, 큐까지 가득 차야 core 이상으로 스레드를 늘린다.
 * 큐가 50처럼 크면 대기 작업이 그만큼 쌓이기 전에는 실효 동시성이 [CORE_POOL_SIZE]에 고정돼 [MAX_POOL_SIZE]까지 늘 일이 없으므로,
 * core를 평상시 목표 동시성으로 두고 큐는 순간 버스트를 흡수할 정도로만 작게 둔다.
 *
 * 이 값은 `daily-fortune-ai`의 격리 executor(`ai-resilience.executor.pool-size` 5 + queueCapacity 10 = 총 15칸, [AiResilienceConfig][com.yapp.todakun.config.AiResilienceConfig])를
 * 홈 화면 자가 치유(`GetTodayFortuneService`)·배치(`GenerateDailyFortunesJobConfig`, chunk 1이라 순차 1건)와 함께 나눠 쓴다는 점을 고려한 것이다.
 * core 6은 그 15칸 중 나머지 자가 치유/배치 몫을 남겨 두면서도 평소 처리량을 확보하고, max 8은 백로그가 쌓였을 때만 짧게 쓰는 여유분이다.
 * 이 예산을 넘겨 거절(`RejectedExecutionException`)되더라도 어댑터가 `DailyFortuneGenerationFailedException`으로 흡수해
 * 배치는 재시도·skip으로, 회원가입 경로는 홈 자가 치유로 각각 만회한다.
 */
@Configuration
class SignupDailyFortuneAsyncConfig {
    @Bean(SIGNUP_DAILY_FORTUNE_EXECUTOR_BEAN_NAME)
    fun signupDailyFortuneTaskExecutor(): AsyncTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            queueCapacity = QUEUE_CAPACITY
            setThreadNamePrefix(THREAD_NAME_PREFIX)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS)
            initialize()
        }

    companion object {
        const val SIGNUP_DAILY_FORTUNE_EXECUTOR_BEAN_NAME = "signupDailyFortuneTaskExecutor"
    }
}
