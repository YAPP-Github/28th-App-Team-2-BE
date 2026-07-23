package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.config.TestContainersConfig
import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.LuckActionScore
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
class GetLuckActionScoresAdapterTest(
    private val luckActionJpaRepository: LuckActionJpaRepository,
) : DescribeSpec(
        {
            val luckActionRepository = LuckActionRepositoryAdapter(luckActionJpaRepository)
            val adapter = GetLuckActionScoresAdapter(luckActionRepository)

            describe("getScores") {
                context("동일한 memberId와 fortuneDate로 저장된 행운 액션이 여러 개이면") {
                    it("각 행운 액션의 id, 운세 카테고리, 점수를 반환한다") {
                        val health = luckActionRepository.save(LuckActionFixture.create())
                        val money =
                            luckActionRepository.save(
                                LuckActionFixture.create(
                                    id = Uuid.generateV7().toJavaUuid(),
                                    fortuneCategory = FortuneCategory.MONEY,
                                ),
                            )

                        val scores = adapter.getScores(health.memberId, health.fortuneDate)

                        scores shouldContainExactlyInAnyOrder
                            listOf(
                                LuckActionScore(id = health.id, fortuneCategory = health.fortuneCategory, score = health.score),
                                LuckActionScore(id = money.id, fortuneCategory = money.fortuneCategory, score = money.score),
                            )
                    }
                }

                context("일치하는 memberId와 fortuneDate가 없으면") {
                    it("빈 목록을 반환한다") {
                        val nonExistentMemberId = Uuid.generateV7().toJavaUuid()

                        val scores = adapter.getScores(nonExistentMemberId, LocalDate.of(2000, 1, 1))

                        scores shouldBe emptyList()
                    }
                }
            }
        },
    )
