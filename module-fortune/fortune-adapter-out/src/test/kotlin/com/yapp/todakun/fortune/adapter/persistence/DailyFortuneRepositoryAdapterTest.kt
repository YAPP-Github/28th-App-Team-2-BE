package com.yapp.todakun.fortune.adapter.persistence

import com.yapp.todakun.fortune.config.TestContainersConfig
import com.yapp.todakun.fortune.fixture.DailyFortuneFixture
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
class DailyFortuneRepositoryAdapterTest(
    private val dailyFortuneJpaRepository: DailyFortuneJpaRepository,
) : DescribeSpec(
        {
            val adapter = DailyFortuneRepositoryAdapter(dailyFortuneJpaRepository)

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
        },
    )
