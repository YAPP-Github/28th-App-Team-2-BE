package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.adapter.oauth.apple.AppleOauthFetcher
import com.yapp.todakun.auth.adapter.oauth.google.GoogleOauthFetcher
import com.yapp.todakun.auth.adapter.oauth.kakao.KakaoOauthFetcher
import com.yapp.todakun.auth.fixture.OauthFixture
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

private const val AUTHORIZATION_CODE = "test-authorization-code"

class OauthAdapterTest :
    DescribeSpec({
        val kakaoOauthFetcher = mockk<KakaoOauthFetcher>()
        val googleOauthFetcher = mockk<GoogleOauthFetcher>()
        val appleOauthFetcher = mockk<AppleOauthFetcher>()
        val oauthAdapter = OauthAdapter(kakaoOauthFetcher, googleOauthFetcher, appleOauthFetcher)

        afterTest { clearMocks(kakaoOauthFetcher, googleOauthFetcher, appleOauthFetcher) }

        describe("fetchProfile") {
            context("provider가 KAKAO면") {
                it("KakaoOauthFetcher에 위임한다") {
                    val profile = OauthFixture.oauthMemberProfile(provider = OauthProvider.KAKAO)
                    every { kakaoOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) } returns profile

                    val result = oauthAdapter.fetchProfile(OauthProvider.KAKAO, OauthFixture.OAUTH_ACCESS_TOKEN)

                    result shouldBe profile
                    verify(exactly = 1) { kakaoOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) }
                    verify(exactly = 0) { googleOauthFetcher.fetchProfile(any()) }
                    verify(exactly = 0) { appleOauthFetcher.fetchProfile(any(), any()) }
                }
            }

            context("provider가 GOOGLE이면") {
                it("GoogleOauthFetcher에 위임한다") {
                    val profile = OauthFixture.oauthMemberProfile(provider = OauthProvider.GOOGLE)
                    every { googleOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) } returns profile

                    val result = oauthAdapter.fetchProfile(OauthProvider.GOOGLE, OauthFixture.OAUTH_ACCESS_TOKEN)

                    result shouldBe profile
                    verify(exactly = 1) { googleOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) }
                    verify(exactly = 0) { kakaoOauthFetcher.fetchProfile(any()) }
                    verify(exactly = 0) { appleOauthFetcher.fetchProfile(any(), any()) }
                }
            }

            context("provider가 APPLE이면") {
                it("AppleOauthFetcher에 authorizationCode와 함께 위임한다") {
                    val profile = OauthFixture.oauthMemberProfile(provider = OauthProvider.APPLE)
                    every {
                        appleOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN, AUTHORIZATION_CODE)
                    } returns profile

                    val result =
                        oauthAdapter.fetchProfile(OauthProvider.APPLE, OauthFixture.OAUTH_ACCESS_TOKEN, AUTHORIZATION_CODE)

                    result shouldBe profile
                    verify(exactly = 1) { appleOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN, AUTHORIZATION_CODE) }
                    verify(exactly = 0) { kakaoOauthFetcher.fetchProfile(any()) }
                    verify(exactly = 0) { googleOauthFetcher.fetchProfile(any()) }
                }
            }
        }
    })
