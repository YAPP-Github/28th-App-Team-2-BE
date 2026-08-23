package com.yapp.todakun.dailyfortune.adapter.redis

import com.yapp.todakun.dailyfortune.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val FORTUNE_DATE: LocalDate = LocalDate.of(2026, 8, 23)

@ExperimentalUuidApi
@DataRedisTest
@Import(TestContainersConfig::class)
class RedisDailyFortuneGenerationLockAdapterTest(
    private val redisTemplate: StringRedisTemplate,
) : DescribeSpec(
        {
            val adapter = RedisDailyFortuneGenerationLockAdapter(redisTemplate)

            describe("tryAcquire") {
                context("처음 선점하면") {
                    it("true를 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        val acquired = adapter.tryAcquire(memberId, FORTUNE_DATE)

                        acquired shouldBe true
                    }
                }

                context("이미 다른 호출자가 선점한 상태면") {
                    it("false를 반환하고 기존 선점을 건드리지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        adapter.tryAcquire(memberId, FORTUNE_DATE)

                        val acquired = adapter.tryAcquire(memberId, FORTUNE_DATE)

                        acquired shouldBe false
                    }
                }

                context("다른 회원·날짜 조합이면") {
                    it("서로 영향을 주지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()
                        adapter.tryAcquire(memberId, FORTUNE_DATE)

                        adapter.tryAcquire(otherMemberId, FORTUNE_DATE) shouldBe true
                        adapter.tryAcquire(memberId, FORTUNE_DATE.plusDays(1)) shouldBe true
                    }
                }
            }

            describe("release") {
                context("선점된 락을 해제하면") {
                    it("이후 같은 조합을 다시 선점할 수 있다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        adapter.tryAcquire(memberId, FORTUNE_DATE)

                        adapter.release(memberId, FORTUNE_DATE)

                        adapter.tryAcquire(memberId, FORTUNE_DATE) shouldBe true
                    }
                }

                context("선점된 락이 없어도") {
                    it("예외 없이 처리된다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.release(memberId, FORTUNE_DATE)

                        adapter.tryAcquire(memberId, FORTUNE_DATE) shouldBe true
                    }
                }
            }
        },
    )
