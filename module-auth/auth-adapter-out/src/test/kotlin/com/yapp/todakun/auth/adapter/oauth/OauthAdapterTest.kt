package com.yapp.todakun.auth.adapter.oauth

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

class OauthAdapterTest :
    DescribeSpec({
        val kakaoOauthFetcher = mockk<KakaoOauthFetcher>()
        val googleOauthFetcher = mockk<GoogleOauthFetcher>()
        val oauthAdapter = OauthAdapter(kakaoOauthFetcher, googleOauthFetcher)

        afterTest { clearMocks(kakaoOauthFetcher, googleOauthFetcher) }

        describe("fetchProfile") {
            context("provider가 KAKAO면") {
                it("KakaoOauthFetcher에 위임한다") {
                    val profile = OauthFixture.oauthMemberProfile(provider = OauthProvider.KAKAO)
                    every { kakaoOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) } returns profile

                    val result = oauthAdapter.fetchProfile(OauthProvider.KAKAO, OauthFixture.OAUTH_ACCESS_TOKEN)

                    result shouldBe profile
                    verify(exactly = 1) { kakaoOauthFetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) }
                    verify(exactly = 0) { googleOauthFetcher.fetchProfile(any()) }
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
                }
            }
        }
    })
