package com.yapp.todakun.dailyfortune.adapter.redis

import com.yapp.todakun.dailyfortune.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
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
                    it("소유권 토큰을 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        val token = adapter.tryAcquire(memberId, FORTUNE_DATE)

                        token.shouldNotBeNull()
                    }
                }

                context("이미 다른 호출자가 선점한 상태면") {
                    it("null을 반환하고 기존 선점을 건드리지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val firstToken = adapter.tryAcquire(memberId, FORTUNE_DATE)

                        val secondToken = adapter.tryAcquire(memberId, FORTUNE_DATE)

                        secondToken.shouldBeNull()
                        // 기존 선점자의 토큰으로는 여전히 정상 해제할 수 있다 = 다른 시도가 값을 덮어쓰지 않았다.
                        adapter.release(memberId, FORTUNE_DATE, firstToken!!)
                        adapter.tryAcquire(memberId, FORTUNE_DATE).shouldNotBeNull()
                    }
                }

                context("같은 조합에 대해 해제 후 재선점하면") {
                    it("이전 호출과 다른 토큰을 발급한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val firstToken = adapter.tryAcquire(memberId, FORTUNE_DATE)!!
                        adapter.release(memberId, FORTUNE_DATE, firstToken)

                        val secondToken = adapter.tryAcquire(memberId, FORTUNE_DATE)

                        secondToken shouldNotBe firstToken
                    }
                }

                context("다른 회원·날짜 조합이면") {
                    it("서로 영향을 주지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()
                        adapter.tryAcquire(memberId, FORTUNE_DATE)

                        adapter.tryAcquire(otherMemberId, FORTUNE_DATE).shouldNotBeNull()
                        adapter.tryAcquire(memberId, FORTUNE_DATE.plusDays(1)).shouldNotBeNull()
                    }
                }
            }

            describe("release") {
                context("선점한 토큰으로 해제하면") {
                    it("이후 같은 조합을 다시 선점할 수 있다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val token = adapter.tryAcquire(memberId, FORTUNE_DATE)!!

                        adapter.release(memberId, FORTUNE_DATE, token)

                        adapter.tryAcquire(memberId, FORTUNE_DATE).shouldNotBeNull()
                    }
                }

                context("이미 만료(또는 해제)돼 다른 호출자가 새로 선점한 뒤 옛 토큰으로 해제를 시도하면") {
                    it("소유권이 없으므로 그 새 선점을 지우지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val staleToken = adapter.tryAcquire(memberId, FORTUNE_DATE)!!
                        adapter.release(memberId, FORTUNE_DATE, staleToken)
                        val newToken = adapter.tryAcquire(memberId, FORTUNE_DATE)!!

                        adapter.release(memberId, FORTUNE_DATE, staleToken)

                        // 새 선점이 여전히 살아있어 세 번째 호출은 선점에 실패해야 한다.
                        adapter.tryAcquire(memberId, FORTUNE_DATE).shouldBeNull()
                        adapter.release(memberId, FORTUNE_DATE, newToken)
                    }
                }

                context("선점된 락이 없어도") {
                    it("예외 없이 처리된다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        adapter.release(memberId, FORTUNE_DATE, "no-such-token")

                        adapter.tryAcquire(memberId, FORTUNE_DATE).shouldNotBeNull()
                    }
                }
            }
        },
    )
