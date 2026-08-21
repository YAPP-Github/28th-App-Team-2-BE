package com.yapp.todakun.auth.adapter.redis.withdrawn

import com.yapp.todakun.auth.config.TestContainersConfig
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import

@DataRedisTest
@Import(TestContainersConfig::class)
class WithdrawnAccountAdapterTest(
    private val withdrawnAccountRepository: WithdrawnAccountRepository,
) : DescribeSpec(
        {
            val properties = WithdrawnAccountProperties(restrictionSeconds = 7_776_000L)
            val adapter = WithdrawnAccountAdapter(withdrawnAccountRepository, properties)

            val provider = OauthProvider.KAKAO
            val providerId = "kakao-1234567890"

            describe("register + isRestricted") {
                context("탈퇴한 SNS 식별자를 등록하면") {
                    it("동일 식별자의 재가입이 제한된다") {
                        adapter.register(provider, providerId)

                        adapter.isRestricted(provider, providerId) shouldBe true
                    }
                }

                context("등록된 적 없는 식별자이면") {
                    it("재가입이 제한되지 않는다") {
                        adapter.isRestricted(OauthProvider.GOOGLE, "never-withdrawn") shouldBe false
                    }
                }

                context("provider가 다르면") {
                    it("같은 providerId라도 별개 식별자로 취급해 제한되지 않는다") {
                        adapter.register(OauthProvider.KAKAO, "shared-id")

                        adapter.isRestricted(OauthProvider.GOOGLE, "shared-id") shouldBe false
                    }
                }

                context("restrictionEnabled가 false이면") {
                    it("탈퇴 기록은 남기되 제한 판별은 건너뛴다(다시 켜면 제한이 되살아난다)") {
                        val disabledAdapter =
                            WithdrawnAccountAdapter(
                                withdrawnAccountRepository,
                                properties.copy(restrictionEnabled = false),
                            )
                        val disabledProviderId = "kakao-restriction-disabled"

                        disabledAdapter.register(provider, disabledProviderId)

                        disabledAdapter.isRestricted(provider, disabledProviderId) shouldBe false
                        adapter.isRestricted(provider, disabledProviderId) shouldBe true
                    }
                }
            }
        },
    )
