package com.yapp.todakun.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

/**
 * AI 포트 호출 시점의 트랜잭션·DB 커넥션 점유 상태 스냅샷(이슈 #59).
 * [transactionActive]가 false이고 [activeConnections]가 0이어야, AI 네트워크 I/O 동안
 * 오케스트레이터가 DB 트랜잭션·커넥션을 점유하지 않는다는 사실이 증명된다.
 */
data class TransactionBoundarySnapshot(
    val transactionActive: Boolean,
    val activeConnections: Int,
)

/** AI 포트 스텁의 `answers` 블록에서 호출해, 바로 그 시점의 트랜잭션·커넥션 점유 상태를 캡처한다. */
fun DataSource.captureTransactionBoundarySnapshot(): TransactionBoundarySnapshot =
    TransactionBoundarySnapshot(
        transactionActive = TransactionSynchronizationManager.isActualTransactionActive(),
        activeConnections = (this as HikariDataSource).hikariPoolMXBean.activeConnections,
    )
