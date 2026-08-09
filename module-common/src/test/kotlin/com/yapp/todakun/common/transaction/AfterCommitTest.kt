package com.yapp.todakun.common.transaction

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.transaction.support.TransactionSynchronizationManager

class AfterCommitTest : DescribeSpec({
    afterTest {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    describe("runAfterCommit") {
        context("활성 트랜잭션이 없으면") {
            it("즉시 실행한다") {
                var executed = false

                runAfterCommit { executed = true }

                executed shouldBe true
            }
        }

        context("활성 트랜잭션이 있으면") {
            it("커밋 전에는 실행하지 않고, 커밋 후에 실행한다") {
                var executed = false
                TransactionSynchronizationManager.initSynchronization()

                runAfterCommit { executed = true }
                executed shouldBe false

                TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
                executed shouldBe true
            }

            it("롤백되면(afterCommit이 호출되지 않으면) 실행하지 않는다") {
                var executed = false
                TransactionSynchronizationManager.initSynchronization()

                runAfterCommit { executed = true }
                TransactionSynchronizationManager.clearSynchronization()

                executed shouldBe false
            }
        }
    }
})
