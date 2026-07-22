package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.config.TestContainersConfig
import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.shared.FortuneCategory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
class LuckActionRepositoryAdapterTest(
    private val luckActionJpaRepository: LuckActionJpaRepository,
) : DescribeSpec(
        {
            val adapter = LuckActionRepositoryAdapter(luckActionJpaRepository)

            fun savedLuckAction() = adapter.save(LuckActionFixture.create())

            describe("save") {
                context("신규 행운 액션을 저장하면") {
                    it("저장된 행운 액션을 반환한다") {
                        val luckAction = LuckActionFixture.create()

                        val saved = adapter.save(luckAction)

                        saved shouldBe luckAction
                    }
                }

                context("이미 존재하는 id로 achieved 상태를 변경해 다시 저장하면") {
                    it("갱신된 achieved 상태로 저장된다") {
                        val luckAction = savedLuckAction()

                        val updated = adapter.save(luckAction.toggle())

                        updated.achieved shouldBe true
                        adapter.findById(luckAction.id)?.achieved shouldBe true
                    }
                }
            }

            describe("findById") {
                context("저장된 id로 조회하면") {
                    it("해당 행운 액션을 반환한다") {
                        val luckAction = savedLuckAction()

                        val found = adapter.findById(luckAction.id)

                        found.shouldNotBeNull()
                        found shouldBe luckAction
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

            describe("findAllByMemberIdAndFortuneDate") {
                context("동일한 memberId와 fortuneDate로 저장된 행운 액션이 여러 개이면") {
                    it("해당하는 모든 행운 액션을 반환한다") {
                        val health = savedLuckAction()
                        val money =
                            adapter.save(
                                LuckActionFixture.create(
                                    id = Uuid.generateV7().toJavaUuid(),
                                    fortuneCategory = FortuneCategory.MONEY,
                                ),
                            )

                        val found = adapter.findAllByMemberIdAndFortuneDate(health.memberId, health.fortuneDate)

                        found shouldContainExactlyInAnyOrder listOf(health, money)
                    }
                }

                context("일치하는 memberId와 fortuneDate가 없으면") {
                    it("빈 목록을 반환한다") {
                        val nonExistentMemberId = Uuid.generateV7().toJavaUuid()

                        val found = adapter.findAllByMemberIdAndFortuneDate(nonExistentMemberId, LocalDate.of(2000, 1, 1))

                        found shouldBe emptyList()
                    }
                }
            }
        },
    )
