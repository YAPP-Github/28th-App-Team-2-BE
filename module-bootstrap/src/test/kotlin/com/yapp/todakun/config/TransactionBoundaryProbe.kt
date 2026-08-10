package com.yapp.todakun.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

/**
 * AI 포트 호출 시점에 **호출 스레드가** 붙들고 있는 트랜잭션·JPA 자원 스냅샷(이슈 #59).
 * 둘 다 false여야 AI 네트워크 I/O 동안 트랜잭션도 DB 커넥션도 점유하지 않는다는 사실이 증명된다.
 */
data class TransactionBoundarySnapshot(
    val transactionActive: Boolean,
    val entityManagerBound: Boolean,
)

/**
 * 스냅샷을 뜨는 테스트 헬퍼. AI 포트 스텁의 `answers` 블록에서 [capture]를 호출해, 바로 그 시점의 상태를 기록한다.
 *
 * 풀 전체 활성 커넥션 수(`HikariPoolMXBean.activeConnections`)는 쓰지 않는다 —
 * 같은 풀을 공유하는 다른 실행까지 함께 세는 값이라 검증하려는 실행 맥락을 특정하지 못하고,
 * 병렬 실행 환경에서 흔들린다(PR #65 리뷰).
 *
 * 대신 현재 스레드에 바인딩된 자원만 본다. JPA에서 트랜잭션 보유는 곧 EntityManager 보유이고,
 * 커넥션은 그 EntityManager를 통해서만 쥐게 되므로 [TransactionBoundarySnapshot.entityManagerBound]가 false라는 것은
 * 이 스레드가 커넥션을 붙들고 있지 않다는 뜻이다.
 */
class TransactionBoundaryProbe(
    private val dataSource: DataSource,
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun capture(): TransactionBoundarySnapshot =
        TransactionBoundarySnapshot(
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive(),
            entityManagerBound =
                TransactionSynchronizationManager.hasResource(entityManagerFactory) ||
                    TransactionSynchronizationManager.hasResource(dataSource),
        )
}
