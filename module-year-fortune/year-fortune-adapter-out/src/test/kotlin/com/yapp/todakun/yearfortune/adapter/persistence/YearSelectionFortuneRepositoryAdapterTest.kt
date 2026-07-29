package com.yapp.todakun.yearfortune.adapter.persistence

import com.yapp.todakun.yearfortune.config.TestContainersConfig
import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class YearSelectionFortuneRepositoryAdapterTest(
    private val yearSelectionFortuneJpaRepository: YearSelectionFortuneJpaRepository,
) : DescribeSpec(
        {
            val adapter = YearSelectionFortuneRepositoryAdapter(yearSelectionFortuneJpaRepository)

            fun savedYearSelectionFortune() =
                yearSelectionFortuneJpaRepository
                    .save(YearSelectionFortuneJpaEntity.fromDomain(YearSelectionFortuneFixture.create()))
                    .toDomain()

            describe("findByMemberIdAndYear") {
                context("저장된 memberId와 year로 조회하면") {
                    it("해당 연도별 운세를 반환한다") {
                        val yearSelectionFortune = savedYearSelectionFortune()

                        val found = adapter.findByMemberIdAndYear(yearSelectionFortune.memberId, yearSelectionFortune.year)

                        found.shouldNotBeNull()
                        found shouldBe yearSelectionFortune
                    }
                }

                context("일치하는 memberId와 year가 없으면") {
                    it("null을 반환한다") {
                        val nonExistentMemberId = Uuid.generateV7().toJavaUuid()

                        val found = adapter.findByMemberIdAndYear(nonExistentMemberId, 2026)

                        found.shouldBeNull()
                    }
                }
            }

            describe("lock") {
                context("같은 트랜잭션 내에서 동일한 (memberId, year)로 재호출하면") {
                    it("재진입 락이므로 예외 없이 통과한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.lock(memberId, 2026)
                        adapter.lock(memberId, 2026)
                    }
                }
            }
        },
    )
