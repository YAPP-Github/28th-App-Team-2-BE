package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.dailyfortune.exception.DailyFortuneEmptyResponseException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.GetMemberIdsPort
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.listener.JobExecutionListener
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.util.UUID

private const val GENERATE_DAILY_FORTUNES_JOB_NAME = "generateDailyFortunesJob"
private const val GENERATE_DAILY_FORTUNES_STEP_NAME = "generateDailyFortunesStep"
private const val MEMBER_ID_PAGE_SIZE = 100
private const val AI_CALL_RETRY_LIMIT = 3L

/**
 * 오늘의 운세 배치 Job/Step 구성.
 * chunk size는 1로 고정한다 — [CreateDailyFortunePort.create]가 회원 1명 단위로 AI 호출·저장을 자체 트랜잭션(@CommandService)으로
 * 커밋하기 때문에, chunk size를 키우면 여러 회원의 커밋이 하나의 Step 트랜잭션에 묶여 회원별 독립 커밋 격리가 깨진다.
 * AI 호출 실패는 대부분 일시적이므로 회원당 최대 [AI_CALL_RETRY_LIMIT]회 재시도하고, 그래도 실패하면 해당 회원만
 * skip해 배치 전체가 중단되지 않게 한다. chunk size가 1이라 재시도 시 재실행 범위도 회원 1명으로 국한되어 안전하다.
 */
@Configuration
class GenerateDailyFortunesJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val getMemberIdsPort: GetMemberIdsPort,
    private val createDailyFortunePort: CreateDailyFortunePort,
) {
    @Bean
    fun generateDailyFortunesJob(
        generateDailyFortunesStep: Step,
        dailyFortuneJobExecutionListener: JobExecutionListener,
    ): Job =
        JobBuilder(GENERATE_DAILY_FORTUNES_JOB_NAME, jobRepository)
            .start(generateDailyFortunesStep)
            .listener(dailyFortuneJobExecutionListener)
            .build()

    @Bean
    fun dailyFortuneJobExecutionListener(): JobExecutionListener = DailyFortuneJobExecutionListener()

    @Bean
    fun generateDailyFortunesStep(
        memberIdItemReader: ItemReader<UUID>,
        dailyFortuneItemProcessor: DailyFortuneItemProcessor,
        dailyFortuneSkipListener: DailyFortuneSkipListener,
    ): Step =
        StepBuilder(GENERATE_DAILY_FORTUNES_STEP_NAME, jobRepository)
            .chunk<UUID, UUID>(1)
            .reader(memberIdItemReader)
            .processor(dailyFortuneItemProcessor)
            .writer(ItemWriter { })
            .faultTolerant()
            .retryLimit(AI_CALL_RETRY_LIMIT)
            .retry(DailyFortuneGenerationFailedException::class.java)
            .retry(DailyFortuneEmptyResponseException::class.java)
            .skipLimit(Long.MAX_VALUE)
            .skip(DailyFortuneGenerationFailedException::class.java)
            .skip(DailyFortuneEmptyResponseException::class.java)
            .listener(dailyFortuneSkipListener)
            .transactionManager(transactionManager)
            .build()

    @Bean
    @StepScope
    fun memberIdItemReader(): ItemReader<UUID> = MemberIdItemReader(getMemberIdsPort, MEMBER_ID_PAGE_SIZE)

    @Bean
    @StepScope
    fun dailyFortuneItemProcessor(): DailyFortuneItemProcessor = DailyFortuneItemProcessor(createDailyFortunePort)

    @Bean
    @StepScope
    fun dailyFortuneSkipListener(): DailyFortuneSkipListener = DailyFortuneSkipListener()
}
