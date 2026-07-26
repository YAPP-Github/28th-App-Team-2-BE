package com.yapp.todakun.auth.adapter.redis.refresh

import com.yapp.todakun.auth.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataRedisTest
@Import(TestContainersConfig::class)
class RefreshTokenAdapterTest(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val redisTemplate: StringRedisTemplate,
) : DescribeSpec(
        {
            val properties = RefreshTokenProperties(expirySeconds = 86400L)
            val adapter = RefreshTokenAdapter(refreshTokenRepository, properties, redisTemplate)
            val memberId = Uuid.generateV7().toJavaUuid()

            describe("issue") {
                context("memberId가 주어지면") {
                    it("토큰을 저장하고 발급 정보를 반환한다") {
                        val issued = adapter.issue(memberId)

                        issued.value.shouldNotBeBlank()
                        issued.expiresInSeconds shouldBe properties.expirySeconds
                        refreshTokenRepository.findById(issued.value).get().memberId shouldBe memberId.toString()
                    }
                }
            }

            describe("consume") {
                context("존재하는 토큰이면") {
                    it("memberId를 반환하고 토큰을 폐기한다") {
                        val issued = adapter.issue(memberId)

                        val consumed = adapter.consume(issued.value)

                        consumed shouldBe memberId
                        refreshTokenRepository.findById(issued.value).isPresent shouldBe false
                    }
                }

                context("존재하지 않는 토큰이면") {
                    it("null을 반환한다") {
                        val nonExistentToken = Uuid.generateV7().toJavaUuid().toString()

                        adapter.consume(nonExistentToken).shouldBeNull()
                    }
                }

                context("이미 소비된 토큰이면") {
                    it("null을 반환한다") {
                        val issued = adapter.issue(memberId)
                        adapter.consume(issued.value)

                        adapter.consume(issued.value).shouldBeNull()
                    }
                }

                context("동일한 토큰으로 동시에 요청하면") {
                    it("한 요청만 성공한다") {
                        val issued = adapter.issue(memberId)
                        val requestCount = 10
                        val executor = Executors.newFixedThreadPool(requestCount)
                        val readyLatch = CountDownLatch(requestCount)
                        val startLatch = CountDownLatch(1)
                        val results =
                            (1..requestCount).map {
                                executor.submit<UUID?> {
                                    readyLatch.countDown()
                                    startLatch.await()
                                    adapter.consume(issued.value)
                                }
                            }

                        readyLatch.await()
                        startLatch.countDown()
                        val consumedResults = results.map { it.get(5, TimeUnit.SECONDS) }
                        executor.shutdown()

                        consumedResults.count { it == memberId } shouldBe 1
                        consumedResults.count { it == null } shouldBe requestCount - 1
                    }
                }
            }

            describe("revokeAll") {
                context("memberId로 여러 토큰을 발급했으면") {
                    it("모두 삭제한다") {
                        val first = adapter.issue(memberId)
                        val second = adapter.issue(memberId)

                        adapter.revokeAll(memberId)

                        refreshTokenRepository.findById(first.value).isPresent shouldBe false
                        refreshTokenRepository.findById(second.value).isPresent shouldBe false
                    }
                }
            }
        },
    )
