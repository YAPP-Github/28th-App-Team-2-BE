package com.yapp.todakun.auth.adapter.redis.onboarding

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.config.TestContainersConfig
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import kotlin.uuid.ExperimentalUuidApi

private const val NON_EXISTENT_TOKEN = "non-existent-token"

@ExperimentalUuidApi
@DataRedisTest
@Import(TestContainersConfig::class)
class OnboardingTokenAdapterTest(
    private val onboardingTokenRepository: OnboardingTokenRepository,
) : DescribeSpec(
        {
            val properties = OnboardingTokenProperties(expirySeconds = 600L)
            val adapter = OnboardingTokenAdapter(onboardingTokenRepository, properties)

            fun profile() =
                OauthMemberProfile(
                    provider = OauthProvider.KAKAO,
                    providerId = "kakao-provider-id",
                    email = "test@example.com",
                )

            describe("issue") {
                context("OAuth 프로필이 주어지면") {
                    it("토큰을 저장하고 발급 정보를 반환한다") {
                        val issued = adapter.issue(profile())

                        issued.value.shouldNotBeBlank()
                        issued.expiresInSeconds shouldBe properties.expirySeconds
                    }
                }
            }

            describe("findProfile") {
                context("저장된 토큰이면") {
                    it("발급 시의 프로필을 복원한다") {
                        val issued = adapter.issue(profile())

                        val found = adapter.findProfile(issued.value)

                        found.shouldNotBeNull()
                        found shouldBe profile()
                    }
                }

                context("존재하지 않는 토큰이면") {
                    it("null을 반환한다") {
                        adapter.findProfile(NON_EXISTENT_TOKEN).shouldBeNull()
                    }
                }
            }

            describe("revoke") {
                context("존재하는 토큰이면") {
                    it("삭제한다") {
                        val issued = adapter.issue(profile())

                        adapter.revoke(issued.value)

                        adapter.findProfile(issued.value).shouldBeNull()
                    }
                }
            }
        },
    )
