package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.config.TestContainersConfig
import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.shared.FortuneCategory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.ZoneId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class LuckyActionNotificationAdapterTest(
    private val luckActionJpaRepository: LuckActionJpaRepository,
) : DescribeSpec(
        {
            val luckActionRepository = LuckActionRepositoryAdapter(luckActionJpaRepository)
            val adapter = LuckyActionNotificationAdapter(luckActionRepository)

            describe("getLuckyActionReminder") {
                context("완료하지 않은 행운 액션이 있으면") {
                    it("미완료 개수를 담은 payload를 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val today = LocalDate.now(SEOUL_ZONE)
                        luckActionRepository.save(
                            LuckActionFixture.create(
                                id = Uuid.generateV7().toJavaUuid(),
                                memberId = memberId,
                                fortuneCategory = FortuneCategory.HEALTH,
                                fortuneDate = today,
                                achieved = false,
                            ),
                        )
                        luckActionRepository.save(
                            LuckActionFixture.create(
                                id = Uuid.generateV7().toJavaUuid(),
                                memberId = memberId,
                                fortuneCategory = FortuneCategory.MONEY,
                                fortuneDate = today,
                                achieved = true,
                            ),
                        )

                        val payload = adapter.getLuckyActionReminder(memberId)

                        payload.shouldNotBeNull()
                        payload.title shouldBe "오늘의 행운 액션"
                        payload.content shouldBe "완료하지 않은 행운 액션이 1개 있어요. 지금 확인해 보세요."
                        payload.deepLink shouldBe "todakun://lucky-action"
                    }
                }

                context("오늘 행운 액션을 전부 완료했으면") {
                    it("null을 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        luckActionRepository.save(
                            LuckActionFixture.create(
                                id = Uuid.generateV7().toJavaUuid(),
                                memberId = memberId,
                                fortuneDate = LocalDate.now(SEOUL_ZONE),
                                achieved = true,
                            ),
                        )

                        adapter.getLuckyActionReminder(memberId).shouldBeNull()
                    }
                }

                context("오늘 생성된 행운 액션이 없으면") {
                    it("null을 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.getLuckyActionReminder(memberId).shouldBeNull()
                    }
                }
            }
        },
    )
