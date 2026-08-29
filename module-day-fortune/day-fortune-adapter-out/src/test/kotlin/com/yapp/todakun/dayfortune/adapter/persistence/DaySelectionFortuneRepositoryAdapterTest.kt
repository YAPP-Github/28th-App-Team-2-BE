package com.yapp.todakun.dayfortune.adapter.persistence

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.config.TestContainersConfig
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class DaySelectionFortuneRepositoryAdapterTest(
    private val daySelectionFortuneJpaRepository: DaySelectionFortuneJpaRepository,
) : DescribeSpec(
        {
            val adapter = DaySelectionFortuneRepositoryAdapter(daySelectionFortuneJpaRepository)

            fun savedDaySelectionFortune() =
                daySelectionFortuneJpaRepository
                    .save(DaySelectionFortuneJpaEntity.fromDomain(DaySelectionFortuneFixture.create()))
                    .toDomain()

            describe("findById") {
                context("저장된 id로 조회하면") {
                    it("해당 택일 운세를 반환한다") {
                        val daySelectionFortune = savedDaySelectionFortune()

                        val found = adapter.findById(daySelectionFortune.id)

                        found.shouldNotBeNull()
                        found shouldBe daySelectionFortune
                    }
                }

                context("일치하는 id가 없으면") {
                    it("null을 반환한다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()

                        val found = adapter.findById(nonExistentId)

                        found.shouldBeNull()
                    }
                }
            }

            describe("findByMemberIdAndPurposeAndTargetDate") {
                context("저장된 memberId·purpose·targetDate로 조회하면") {
                    it("해당 택일 운세를 반환한다") {
                        val daySelectionFortune = savedDaySelectionFortune()

                        val found =
                            adapter.findByMemberIdAndPurposeAndTargetDate(
                                daySelectionFortune.memberId,
                                daySelectionFortune.purpose,
                                daySelectionFortune.targetDate,
                            )

                        found.shouldNotBeNull()
                        found shouldBe daySelectionFortune
                    }
                }

                context("일치하는 memberId·purpose·targetDate가 없으면") {
                    it("null을 반환한다") {
                        val nonExistentMemberId = Uuid.generateV7().toJavaUuid()

                        val found =
                            adapter.findByMemberIdAndPurposeAndTargetDate(
                                nonExistentMemberId,
                                DaySelectionPurpose.TRAVEL,
                                LocalDate.of(2026, 8, 1),
                            )

                        found.shouldBeNull()
                    }
                }
            }

            describe("lock") {
                context("같은 트랜잭션 내에서 동일한 (memberId, purpose, targetDate)로 재호출하면") {
                    it("재진입 락이므로 예외 없이 통과한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.lock(memberId, DaySelectionPurpose.TRAVEL, LocalDate.of(2026, 8, 1))
                        adapter.lock(memberId, DaySelectionPurpose.TRAVEL, LocalDate.of(2026, 8, 1))
                    }
                }
            }
        },
    )
