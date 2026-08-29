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
import java.time.LocalDate
import java.time.ZoneId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class, JpaAuditingTestConfig::class)
class DailyFortuneNotificationAdapterTest(
    private val dailyFortuneJpaRepository: DailyFortuneJpaRepository,
) : DescribeSpec(
        {
            val dailyFortuneRepository = DailyFortuneRepositoryAdapter(dailyFortuneJpaRepository)
            val adapter = DailyFortuneNotificationAdapter(dailyFortuneRepository)

            describe("getMorningReport") {
                context("당일 운세가 생성되어 있으면") {
                    it("점수 요약을 담은 payload를 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        dailyFortuneRepository.save(
                            DailyFortuneFixture.create(
                                id = Uuid.generateV7().toJavaUuid(),
                                memberId = memberId,
                                fortuneDate = LocalDate.now(SEOUL_ZONE),
                                score = 80,
                                title = "오늘은 활기찬 하루가 될 거예요",
                            ),
                        )

                        val payload = adapter.getMorningReport(memberId)

                        payload.shouldNotBeNull()
                        payload.title shouldBe "오늘은 활기찬 하루가 될 거예요"
                        payload.content shouldBe "오늘의 운세 점수는 80점이에요. 지금 확인해 보세요."
                        payload.deepLink shouldBe "todakun://fortune/today"
                    }
                }

                context("당일 운세가 아직 생성되지 않았으면") {
                    it("null을 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.getMorningReport(memberId).shouldBeNull()
                    }
                }
            }
        },
    )
