package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NotificationDeliveryFailure
import com.yapp.todakun.notification.config.TestContainersConfig
import com.yapp.todakun.shared.NotificationType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class NotificationDeliveryFailureRepositoryAdapterTest(
    private val notificationDeliveryFailureJpaRepository: NotificationDeliveryFailureJpaRepository,
) : DescribeSpec(
        {
            val adapter = NotificationDeliveryFailureRepositoryAdapter(notificationDeliveryFailureJpaRepository)

            fun failure(nextRetryAt: Instant) =
                NotificationDeliveryFailure.create(
                    memberId = Uuid.generateV7().toJavaUuid(),
                    notificationId = Uuid.generateV7().toJavaUuid(),
                    type = NotificationType.FORTUNE,
                    title = "제목",
                    content = "본문",
                    deepLink = "fortune/today",
                    nextRetryAt = nextRetryAt,
                )

            describe("findDue") {
                context("now 이전에 재시도 예정인 건만") {
                    it("오래된 순으로 조회하고, 아직 도래하지 않은 건은 제외한다") {
                        val now = Instant.now()
                        val due = adapter.save(failure(now.minus(10, ChronoUnit.MINUTES)))
                        adapter.save(failure(now.plus(10, ChronoUnit.MINUTES)))

                        val result = adapter.findDue(now, limit = 10)

                        result.map { it.id } shouldBe listOf(due.id)
                    }
                }

                context("도래한 건이 없으면") {
                    it("빈 목록을 반환한다") {
                        adapter.save(failure(Instant.now().plus(1, ChronoUnit.HOURS)))

                        adapter.findDue(Instant.now(), limit = 10).shouldBeEmpty()
                    }
                }
            }

            describe("deleteById") {
                it("해당 건을 삭제한다") {
                    val saved = adapter.save(failure(Instant.now().minus(1, ChronoUnit.MINUTES)))

                    adapter.deleteById(saved.id)

                    adapter.findDue(Instant.now(), limit = 10).shouldBeEmpty()
                }
            }
        },
    )
