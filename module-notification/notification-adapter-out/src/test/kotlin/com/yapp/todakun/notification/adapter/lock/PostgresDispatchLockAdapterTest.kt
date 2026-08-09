package com.yapp.todakun.notification.adapter.lock

import com.yapp.todakun.notification.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

// 락 보유측이 실제로 락을 잡을 때까지 기다리는 시간.
private const val AWAIT_TIMEOUT_SECONDS = 10L

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class PostgresDispatchLockAdapterTest(
    private val dataSource: DataSource,
) : DescribeSpec(
        {
            val adapter = PostgresDispatchLockAdapter(dataSource)

            describe("tryRun") {
                context("아무도 같은 키의 락을 보유하지 않았으면") {
                    it("block을 실행하고 그 결과를 반환한다") {
                        val result = adapter.tryRun(1_001L) { "done" }

                        result shouldBe "done"
                    }
                }

                context("다른 곳에서 이미 같은 키의 락을 보유 중이면(Blue/Green 동시 실행 시나리오)") {
                    it("block을 실행하지 않고 null을 반환한다") {
                        val holderAcquired = CountDownLatch(1)
                        val releaseHolder = CountDownLatch(1)
                        val executor = Executors.newSingleThreadExecutor()

                        try {
                            executor.submit {
                                adapter.tryRun(1_002L) {
                                    holderAcquired.countDown()
                                    releaseHolder.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                }
                            }
                            holderAcquired.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe true

                            adapter.tryRun(1_002L) { "should not run" }.shouldBeNull()
                        } finally {
                            releaseHolder.countDown()
                            executor.shutdown()
                        }
                    }
                }

                context("먼저 보유했던 쪽이 락을 해제한 뒤에는") {
                    it("이후 시도가 락을 획득할 수 있다") {
                        adapter.tryRun(1_003L) { "first" }

                        val result = adapter.tryRun(1_003L) { "second" }

                        result shouldBe "second"
                    }
                }
            }
        },
    )
