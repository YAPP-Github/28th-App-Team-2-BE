package com.yapp.todakun.dailyfortune.adapter.persistence

import com.yapp.todakun.dailyfortune.config.JpaAuditingTestConfig
import com.yapp.todakun.dailyfortune.config.TestContainersConfig
import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

// 락 해제를 기다리는 시간. 대기 중인 트랜잭션이 락을 얻기까지 충분한 여유를 둔다.
private const val ACQUIRE_TIMEOUT_SECONDS = 10L

// 락이 실제로 차단되는지 확인하는 시간. 이 시간 안에 통과하면 직렬화가 되지 않은 것이다.
private const val BLOCKED_TIMEOUT_SECONDS = 1L

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class, JpaAuditingTestConfig::class)
class DailyFortuneRepositoryAdapterTest(
    private val dailyFortuneJpaRepository: DailyFortuneJpaRepository,
    private val transactionManager: PlatformTransactionManager,
) : DescribeSpec(
        {
            val adapter = DailyFortuneRepositoryAdapter(dailyFortuneJpaRepository)
            val transactionTemplate = TransactionTemplate(transactionManager)

            fun savedDailyFortune() = adapter.save(DailyFortuneFixture.create())

            describe("save") {
                context("신규 오늘의 운세를 저장하면") {
                    it("저장된 오늘의 운세를 반환한다") {
                        val dailyFortune = DailyFortuneFixture.create()

                        val saved = adapter.save(dailyFortune)

                        saved shouldBe dailyFortune
                    }
                }
            }

            describe("findById") {
                context("저장된 id로 조회하면") {
                    it("해당 오늘의 운세를 반환한다") {
                        val dailyFortune = savedDailyFortune()

                        val found = adapter.findById(dailyFortune.id)

                        found.shouldNotBeNull()
                        found shouldBe dailyFortune
                    }
                }

                context("존재하지 않는 id로 조회하면") {
                    it("null을 반환한다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()

                        val found = adapter.findById(nonExistentId)

                        found.shouldBeNull()
                    }
                }
            }

            describe("findByMemberIdAndFortuneDate") {
                context("저장된 memberId와 fortuneDate로 조회하면") {
                    it("해당 오늘의 운세를 반환한다") {
                        val dailyFortune = savedDailyFortune()

                        val found = adapter.findByMemberIdAndFortuneDate(dailyFortune.memberId, dailyFortune.fortuneDate)

                        found.shouldNotBeNull()
                        found shouldBe dailyFortune
                    }
                }

                context("일치하는 memberId와 fortuneDate가 없으면") {
                    it("null을 반환한다") {
                        val nonExistentMemberId = Uuid.generateV7().toJavaUuid()

                        val found = adapter.findByMemberIdAndFortuneDate(nonExistentMemberId, LocalDate.of(2000, 1, 1))

                        found.shouldBeNull()
                    }
                }
            }

            describe("lock") {
                context("같은 트랜잭션 내에서 동일한 (memberId, fortuneDate)로 재호출하면") {
                    it("재진입 락이므로 예외 없이 통과한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val fortuneDate = LocalDate.of(2026, 6, 24)

                        transactionTemplate.executeWithoutResult {
                            adapter.lock(memberId, fortuneDate)
                            adapter.lock(memberId, fortuneDate)
                        }
                    }
                }

                context("다른 트랜잭션이 동일한 (memberId, fortuneDate) 락을 보유한 동안에는") {
                    it("선행 트랜잭션이 커밋될 때까지 대기했다가 락을 획득한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val fortuneDate = LocalDate.of(2026, 6, 25)
                        val firstAcquired = CountDownLatch(1)
                        val allowFirstCommit = CountDownLatch(1)
                        val secondAcquired = CountDownLatch(1)
                        val executor = Executors.newFixedThreadPool(2)

                        try {
                            // 선행 트랜잭션: 락을 잡은 채로 커밋을 미룬다.
                            executor.submit {
                                transactionTemplate.executeWithoutResult {
                                    adapter.lock(memberId, fortuneDate)
                                    firstAcquired.countDown()
                                    allowFirstCommit.await(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                }
                            }
                            firstAcquired.await(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe true

                            // 후행 트랜잭션: 같은 키로 락을 시도한다.
                            executor.submit {
                                transactionTemplate.executeWithoutResult {
                                    adapter.lock(memberId, fortuneDate)
                                    secondAcquired.countDown()
                                }
                            }

                            // 선행 트랜잭션이 락을 쥐고 있는 동안에는 진입하지 못한다.
                            secondAcquired.await(BLOCKED_TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe false

                            // 선행 트랜잭션이 커밋되면 pg_advisory_xact_lock이 해제되어 대기가 풀린다.
                            allowFirstCommit.countDown()
                            secondAcquired.await(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe true
                        } finally {
                            allowFirstCommit.countDown()
                            executor.shutdown()
                        }
                    }
                }
            }
        },
    )
