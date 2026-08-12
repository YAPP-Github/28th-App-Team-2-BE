package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NoticeDispatchHistory
import com.yapp.todakun.notification.config.TestContainersConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class NoticeDispatchHistoryRepositoryAdapterTest(
    private val jpaRepository: NoticeDispatchHistoryJpaRepository,
) : DescribeSpec(
        {
            val adapter = NoticeDispatchHistoryRepositoryAdapter(jpaRepository)

            fun history(idempotencyKey: String) =
                NoticeDispatchHistory.create(
                    idempotencyKey = idempotencyKey,
                    title = "제목",
                    content = "본문",
                    deepLink = "notice/1",
                    dispatchedAt = Instant.now(),
                )

            describe("saveIfAbsent") {
                context("새 idempotencyKey면") {
                    it("true를 반환하고 저장한다") {
                        val key = "key-${Uuid.generateV7()}"

                        val claimed = adapter.saveIfAbsent(history(key))

                        claimed shouldBe true
                        jpaRepository.existsByIdempotencyKey(key) shouldBe true
                    }
                }

                context("같은 idempotencyKey를 다시 저장하면") {
                    it("false를 반환하고 행이 늘지 않는다") {
                        val key = "key-${Uuid.generateV7()}"
                        adapter.saveIfAbsent(history(key))
                        val countAfterFirstSave = jpaRepository.count()

                        val claimed = adapter.saveIfAbsent(history(key))

                        claimed shouldBe false
                        jpaRepository.count() shouldBe countAfterFirstSave
                    }
                }

                // 스레드 기반 동시성 테스트는 @DataJpaTest의 롤백 트랜잭션 안에서는 신뢰할 수 없어(같은 트랜잭션을
                // 공유하거나 커넥션이 격리되지 않을 수 있음) DB 유니크 제약이 실제로 동작하는지를 직접 검증한다.
                // 이 제약이 saveIfAbsent의 선조회~INSERT 사이 경합(동시 실행 2건이 동시에 존재 확인을 통과하는 경우)에도
                // 이력이 중복 생성될 수 없다는 것을 보장하는 최후 방어선이다.
                context("DB 유니크 제약은") {
                    it("같은 idempotencyKey를 가진 서로 다른 id의 엔티티 2건을 동시에 저장하려 하면 막는다") {
                        val key = "key-${Uuid.generateV7()}"
                        val first = NoticeDispatchHistoryJpaEntity.fromDomain(history(key))
                        val second = NoticeDispatchHistoryJpaEntity.fromDomain(history(key))
                        jpaRepository.saveAndFlush(first)

                        shouldThrow<DataIntegrityViolationException> {
                            jpaRepository.saveAndFlush(second)
                        }
                    }
                }
            }
        },
    )
