package com.yapp.todakun.dailyfortune.application.batch

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration
import org.springframework.batch.core.repository.JobRepository
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer
import org.springframework.boot.sql.init.DatabaseInitializationMode
import org.springframework.boot.sql.init.DatabaseInitializationSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import javax.sql.DataSource

/**
 * Boot 4의 `BatchAutoConfiguration`은 기본값이 인메모리 JobRepository다
 * (spring-boot-batch `BatchAutoConfiguration` Javadoc: "using an in-memory store")
 * 앱이 재시작되면 Job/Step 진행 상태가 사라져 이슈가 원하는 "중간 실패 시 재시작 가능"이 실제로는 동작하지 않는다.
 * [JdbcDefaultBatchConfiguration]을 상속해 JDBC 기반 jobRepository/jobOperator 빈으로 오버라이드한다.
 * (`BatchAutoConfiguration`은 `@ConditionalOnMissingBean(DefaultBatchConfiguration)`이라 이 빈이 있으면 자동으로 양보한다).
 * `GenerateDailyFortunesJobConfig`가 이 클래스와 같이 jobRepository를 상속하면 자기 생성자가 자기 자신이 만드는 빈을 요구하는 순환 참조가 되므로, 인프라 전용 클래스로 분리한다.
 * Boot 4는 `spring.batch.jdbc.initialize-schema` 프로퍼티를 더 이상 제공하지 않으므로, spring-batch-core가 내장한 스키마 스크립트를 직접 실행해 메타데이터 테이블을 생성한다.
 * jobRepository()가 부모 클래스 코드에서 곧바로 메타데이터 테이블을 조회하므로, `@DependsOn`으로 스키마 초기화가 먼저 끝나도록 순서를 강제한다(없으면 "relation ... does not exist"로 기동 실패).
 * DatabaseInitializationSettings의 기본 mode는 EMBEDDED라 Postgres 같은 외부 DB에서는 조용히 스크립트를 건너뛰므로 ALWAYS로 명시해야 한다.
 */
@Configuration
class BatchJdbcConfig : JdbcDefaultBatchConfiguration() {
    @Bean
    fun batchSchemaInitializer(dataSource: DataSource): DataSourceScriptDatabaseInitializer {
        val settings =
            DatabaseInitializationSettings().apply {
                schemaLocations = listOf("classpath:org/springframework/batch/core/schema-postgresql.sql")
                mode = DatabaseInitializationMode.ALWAYS
                // 최초 기동 이후에는 테이블이 이미 있어 매 기동마다 재실행 시 중복 생성 에러가 나므로 계속 진행시킨다.
                isContinueOnError = true
            }
        return DataSourceScriptDatabaseInitializer(dataSource, settings)
    }

    @Bean
    @DependsOn("batchSchemaInitializer")
    override fun jobRepository(): JobRepository = super.jobRepository()
}
