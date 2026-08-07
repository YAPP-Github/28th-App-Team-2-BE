package com.yapp.todakun.dailyfortune.application.batch

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration
import org.springframework.batch.core.repository.JobRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer
import org.springframework.boot.sql.init.DatabaseInitializationMode
import org.springframework.boot.sql.init.DatabaseInitializationSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import java.sql.Connection
import javax.sql.DataSource

private const val BATCH_SCHEMA_LOCATION = "classpath:org/springframework/batch/core/schema-postgresql.sql"
private const val BATCH_REPRESENTATIVE_TABLE = "batch_job_instance"
private const val BATCH_SCHEMA_INITIALIZER_BEAN = "batchSchemaInitializer"

// pg_advisory_lock 키(임의 상수). 이 락은 스키마 초기화 직렬화 전용이므로 다른 advisory lock 용도와 겹치지만 않으면 된다.
private const val BATCH_SCHEMA_ADVISORY_LOCK_KEY = 8_412_037_501L

/**
 * Boot 4의 `BatchAutoConfiguration`은 기본값이 인메모리 JobRepository다
 * (spring-boot-batch `BatchAutoConfiguration` Javadoc: "using an in-memory store")
 * 앱이 재시작되면 Job/Step 진행 상태가 사라져 이슈가 원하는 "중간 실패 시 재시작 가능"이 실제로는 동작하지 않는다.
 * [JdbcDefaultBatchConfiguration]을 상속해 JDBC 기반 jobRepository/jobOperator 빈으로 오버라이드한다.
 * (`BatchAutoConfiguration`은 `@ConditionalOnMissingBean(DefaultBatchConfiguration)`이라 이 빈이 있으면 자동으로 양보한다).
 * `GenerateDailyFortunesJobConfig`가 이 클래스와 같이 jobRepository를 상속하면 자기 생성자가 자기 자신이 만드는 빈을 요구하는 순환 참조가 되므로, 인프라 전용 클래스로 분리한다.
 * `JdbcDefaultBatchConfiguration`을 상속하면 Spring Boot Batch JDBC 자동 설정과 스키마 초기화가 백오프하므로, 스키마 스크립트를 직접 실행해 메타데이터 테이블을 생성한다.
 * jobRepository()가 부모 클래스 코드에서 곧바로 메타데이터 테이블을 조회하므로, `@DependsOn`으로 스키마 초기화가 먼저 끝나도록 순서를 강제한다(없으면 "relation ... does not exist"로 기동 실패).
 * DatabaseInitializationSettings의 기본 mode는 EMBEDDED라 Postgres 같은 외부 DB에서는 조용히 스크립트를 건너뛰므로 프로필별로 `batch.schema.initialization-mode`에 명시한다
 * (운영은 배포 전 수동으로 스키마를 만들고 NEVER로 자동 실행을 막는다).
 * spring-batch-core가 내장한 schema-postgresql.sql은 IF NOT EXISTS가 없어 테이블이 이미 있는 상태에서 재실행하면 "relation already exists"가 발생한다.
 * 스크립트를 직접 포크해 IF NOT EXISTS를 추가하면 라이브러리 스키마 변경(컬럼 추가 등)을 조용히 놓칠 위험이 있으므로,
 * 대신 [batchTablesExist]로 대표 테이블(BATCH_JOB_INSTANCE) 존재 여부를 먼저 확인해 이미 있으면 NEVER로 스크립트 실행 자체를 건너뛴다.
 * isContinueOnError로 에러를 삼키면 "relation already exists"뿐 아니라 진짜 스키마 실패까지 조용히 통과시켜
 * jobRepository() 실행 시점에야 "relation ... does not exist"로 뒤늦게 실패하므로 사용하지 않는다.
 * blue/green 배포 전환 구간에는 신·구 인스턴스가 동시에 떠 있어, [batchTablesExist] 체크와 스크립트 실행 사이에 경쟁 조건이 생길 수 있다
 * (최초 배포처럼 테이블이 아직 없을 때 두 인스턴스가 모두 false를 읽고 동시에 CREATE TABLE을 시도하면 한쪽이 "relation already exists"로 기동 실패한다).
 * `pg_advisory_lock`으로 체크~실행 구간 전체를 감싸 인스턴스 간 기동을 직렬화한다. 세션 단위 락이라 커넥션 풀에 그대로 반납하면 락이 새어나가므로,
 * 사용이 끝나면 명시적으로 `pg_advisory_unlock`을 호출해 반납 전에 풀어준다.
 */
@Configuration
@EnableConfigurationProperties(BatchSchemaProperties::class)
class BatchJdbcConfig(
    private val batchSchemaProperties: BatchSchemaProperties,
) : JdbcDefaultBatchConfiguration() {
    @Bean(BATCH_SCHEMA_INITIALIZER_BEAN)
    fun batchSchemaInitializer(dataSource: DataSource): DataSourceScriptDatabaseInitializer {
        val settings = DatabaseInitializationSettings().apply { schemaLocations = listOf(BATCH_SCHEMA_LOCATION) }
        val initializer = DataSourceScriptDatabaseInitializer(dataSource, settings)
        withAdvisorySchemaLock(dataSource) { connection ->
            settings.mode =
                if (batchTablesExist(connection)) DatabaseInitializationMode.NEVER else batchSchemaProperties.initializationMode
            initializer.initializeDatabase()
        }
        // Spring이 InitializingBean.afterPropertiesSet()으로 initializeDatabase()를 한 번 더 호출하므로,
        // 위에서 이미 스크립트를 실행했다면 중복 실행되지 않도록 NEVER로 되돌려 둔다.
        return initializer.also { settings.mode = DatabaseInitializationMode.NEVER }
    }

    @Bean
    @DependsOn(BATCH_SCHEMA_INITIALIZER_BEAN)
    override fun jobRepository(): JobRepository = super.jobRepository()

    private fun withAdvisorySchemaLock(
        dataSource: DataSource,
        block: (Connection) -> Unit,
    ) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute("SELECT pg_advisory_lock($BATCH_SCHEMA_ADVISORY_LOCK_KEY)") }
            try {
                block(connection)
            } finally {
                connection.createStatement().use { it.execute("SELECT pg_advisory_unlock($BATCH_SCHEMA_ADVISORY_LOCK_KEY)") }
            }
        }
    }

    private fun batchTablesExist(connection: Connection): Boolean =
        connection.metaData.getTables(null, null, BATCH_REPRESENTATIVE_TABLE, arrayOf("TABLE")).use { it.next() }
}
