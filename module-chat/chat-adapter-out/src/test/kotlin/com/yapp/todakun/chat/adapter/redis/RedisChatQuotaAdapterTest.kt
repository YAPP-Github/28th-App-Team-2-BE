package com.yapp.todakun.chat.adapter.redis

import com.yapp.todakun.chat.config.TestContainersConfig
import com.yapp.todakun.chat.exception.ChatDailyQuotaExceededException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val DAILY_FREE_CHAT_LIMIT = 100000
private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

// 어댑터가 (핫픽스로) 하루 무료 채팅 한도를 100000까지 임시로 올려둔 상태라 실제 reserve() 반복 호출로는
// 한도 초과 상황을 재현할 수 없다. 어댑터 내부 키 포맷(private)을 테스트에서 동일하게 재구현해 Redis에 직접 시딩한다.
private fun quotaKey(memberId: UUID): String = "chat:quota:$memberId:${LocalDate.now(ZONE).format(DATE_KEY_FORMATTER)}"

@ExperimentalUuidApi
@DataRedisTest
@Import(TestContainersConfig::class)
class RedisChatQuotaAdapterTest(
    private val redisTemplate: StringRedisTemplate,
) : DescribeSpec(
        {
            val adapter = RedisChatQuotaAdapter(redisTemplate)

            describe("reserve") {
                context("최초 호출이면") {
                    it("사용량 1을 반환하고 자정까지 TTL을 설정한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        val status = adapter.reserve(memberId)

                        status.used shouldBe 1
                        status.limit shouldBe DAILY_FREE_CHAT_LIMIT
                        (redisTemplate.getExpire(quotaKey(memberId)) > 0) shouldBe true
                    }
                }

                context("이미 사용 중이면") {
                    it("사용량을 누적 증가시킨다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        adapter.reserve(memberId)
                        adapter.reserve(memberId)

                        val status = adapter.reserve(memberId)

                        status.used shouldBe 3
                    }
                }

                context("한도를 이미 소진했으면") {
                    it("예외를 던지고 사용량을 더 늘리지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        redisTemplate.opsForValue().set(quotaKey(memberId), DAILY_FREE_CHAT_LIMIT.toString())

                        shouldThrow<ChatDailyQuotaExceededException> { adapter.reserve(memberId) }

                        adapter.getStatus(memberId).used shouldBe DAILY_FREE_CHAT_LIMIT
                    }
                }
            }

            describe("refund") {
                context("사용량이 남아있으면") {
                    it("사용량을 1 감소시킨다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        adapter.reserve(memberId)
                        adapter.reserve(memberId)

                        adapter.refund(memberId)

                        adapter.getStatus(memberId).used shouldBe 1
                    }
                }

                context("감소 후 0 이하가 되면") {
                    it("키를 삭제한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        adapter.reserve(memberId)

                        adapter.refund(memberId)

                        redisTemplate.opsForValue().get(quotaKey(memberId)).shouldBeNull()
                        adapter.getStatus(memberId).used shouldBe 0
                    }
                }

                context("예약 없이 호출해도") {
                    it("예외 없이 처리되고 이후 조회 시 사용량은 0이다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.refund(memberId)

                        adapter.getStatus(memberId).used shouldBe 0
                    }
                }
            }

            describe("getStatus") {
                context("키가 없으면") {
                    it("사용량 0을 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.getStatus(memberId).used shouldBe 0
                    }
                }

                context("키가 있으면") {
                    it("저장된 사용량을 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        adapter.reserve(memberId)
                        adapter.reserve(memberId)

                        adapter.getStatus(memberId).used shouldBe 2
                    }
                }
            }
        },
    )
