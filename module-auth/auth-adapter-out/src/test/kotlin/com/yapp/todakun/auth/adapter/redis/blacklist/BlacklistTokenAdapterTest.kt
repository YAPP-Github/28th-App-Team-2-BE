package com.yapp.todakun.auth.adapter.redis.blacklist

import com.yapp.todakun.auth.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataRedisTest
@Import(TestContainersConfig::class)
class BlacklistTokenAdapterTest(
    private val blacklistTokenRepository: BlacklistTokenRepository,
) : DescribeSpec(
        {
            val adapter = BlacklistTokenAdapter(blacklistTokenRepository)

            describe("blacklist") {
                context("jti와 ttl이 주어지면") {
                    it("저장하고 isBlacklisted가 true를 반환한다") {
                        val jti = Uuid.generateV7().toJavaUuid().toString()

                        adapter.blacklist(jti, remainingTtlSeconds = 3600L)

                        adapter.isBlacklisted(jti) shouldBe true
                    }
                }
            }

            describe("isBlacklisted") {
                context("블랙리스트에 등록된 적 없는 jti이면") {
                    it("false를 반환한다") {
                        val newJti = Uuid.generateV7().toJavaUuid().toString()

                        adapter.isBlacklisted(newJti) shouldBe false
                    }
                }
            }
        },
    )
