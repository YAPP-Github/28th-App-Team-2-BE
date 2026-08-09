package com.yapp.todakun.common.transaction

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 활성 트랜잭션이 커밋된 뒤에만 [action]을 실행한다. 캐시 무효화처럼 DB 변경 결과와 순서를 맞춰야 하는
 * 후속 작업을, 커밋 전 다른 트랜잭션이 아직 이전 데이터로 캐시를 다시 채우는 경쟁이나 롤백 시 불필요한
 * 실행으로부터 보호한다(이슈 #56). 트랜잭션 동기화가 비활성 상태(트랜잭션 밖)면 즉시 실행한다.
 */
fun runAfterCommit(action: () -> Unit) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        action()
        return
    }

    TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
            override fun afterCommit() = action()
        },
    )
}
