package com.yapp.todakun.notification.adapter.lock

import com.yapp.todakun.notification.port.outbound.DispatchLockPort
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * pg_try_advisory_lock 기반 세션 스코프 advisory lock.
 * HikariCP 풀에서 락 획득과 해제가 서로 다른 물리 커넥션에 걸리면 락이 새어나가므로
 * (pg_advisory_unlock은 락을 잡은 바로 그 커넥션에서만 유효하다),
 * dataSource에서 커넥션을 하나 직접 꺼내 획득~[block] 실행~해제를 전부 같은 커넥션에서 수행한다
 * (daily-fortune 모듈 BatchJdbcConfig.withAdvisorySchemaLock과 동일 기법).
 */
@Component
class PostgresDispatchLockAdapter(
    private val dataSource: DataSource,
) : DispatchLockPort {
    override fun <T> tryRun(
        key: Long,
        block: () -> T,
    ): T? =
        dataSource.connection.use { connection ->
            val acquired =
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT pg_try_advisory_lock($key)").use { it.next() && it.getBoolean(1) }
                }
            if (!acquired) {
                return@use null
            }

            try {
                block()
            } finally {
                connection.createStatement().use { it.execute("SELECT pg_advisory_unlock($key)") }
            }
        }
}
