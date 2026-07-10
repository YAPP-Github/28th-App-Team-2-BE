package com.yapp.todakun.auth.adapter.redis.refresh

import com.yapp.todakun.auth.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataRedisTest
@Import(TestContainersConfig::class)
class RefreshTokenAdapterTest(
    private val refreshTokenRepository: RefreshTokenRepository,
) : DescribeSpec(
        {
            val properties = RefreshTokenProperties(expirySeconds = 86400L)
            val adapter = RefreshTokenAdapter(refreshTokenRepository, properties)
            val memberId = Uuid.generateV7().toJavaUuid()

            describe("issue") {
                context("memberId가 주어지면") {
                    it("토큰을 저장하고 발급 정보를 반환한다") {
                        val issued = adapter.issue(memberId)

                        issued.value.shouldNotBeBlank()
                        issued.expiresInSeconds shouldBe properties.expirySeconds
                        adapter.findMemberId(issued.value) shouldBe memberId
                    }
                }
            }

            describe("findMemberId") {
                context("존재하지 않는 토큰이면") {
                    it("null을 반환한다") {
                        val nonExistentToken = Uuid.generateV7().toJavaUuid().toString()

                        adapter.findMemberId(nonExistentToken).shouldBeNull()
                    }
                }
            }

            describe("revoke") {
                context("존재하는 토큰이면") {
                    it("삭제한다") {
                        val issued = adapter.issue(memberId)

                        adapter.revoke(issued.value)

                        adapter.findMemberId(issued.value).shouldBeNull()
                    }
                }
            }

            describe("revokeAll") {
                context("memberId로 여러 토큰을 발급했으면") {
                    it("모두 삭제한다") {
                        val first = adapter.issue(memberId)
                        val second = adapter.issue(memberId)

                        adapter.revokeAll(memberId)

                        adapter.findMemberId(first.value).shouldBeNull()
                        adapter.findMemberId(second.value).shouldBeNull()
                    }
                }
            }
        },
    )
