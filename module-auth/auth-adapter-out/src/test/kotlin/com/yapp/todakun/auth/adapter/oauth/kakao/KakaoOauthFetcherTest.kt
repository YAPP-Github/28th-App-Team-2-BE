package com.yapp.todakun.auth.adapter.oauth.kakao

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.exception.OauthProviderUnavailableException
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.auth.fixture.KakaoOauthFixture
import com.yapp.todakun.auth.fixture.OauthFixture
import com.yapp.todakun.common.exception.UnauthorizedException
import com.yapp.todakun.shared.OauthProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

class KakaoOauthFetcherTest :
    DescribeSpec({
        val restClient = mockk<RestClient>()
        val requestHeadersUriSpec = mockk<RestClient.RequestHeadersUriSpec<*>>()
        val responseSpec = mockk<RestClient.ResponseSpec>()
        val properties = KakaoOauthFixture.properties()
        val fetcher = KakaoOauthFetcher(restClient, properties)

        afterTest { clearMocks(restClient, requestHeadersUriSpec, responseSpec) }

        fun stubRequest() {
            every { restClient.get() } returns requestHeadersUriSpec
            every { requestHeadersUriSpec.uri(properties.userMeUri) } returns requestHeadersUriSpec
            every {
                requestHeadersUriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer ${OauthFixture.OAUTH_ACCESS_TOKEN}")
            } returns requestHeadersUriSpec
        }

        fun stubResponse(response: KakaoMemberInfoResponse) {
            stubRequest()
            every { requestHeadersUriSpec.retrieve() } returns responseSpec
            every { responseSpec.hint(any(), any()) } returns responseSpec
            every {
                responseSpec.body(any<ParameterizedTypeReference<KakaoMemberInfoResponse>>())
            } returns response
        }

        fun stubFailure(exception: Throwable) {
            stubRequest()
            every { requestHeadersUriSpec.retrieve() } throws exception
        }

        describe("fetchProfile") {
            context("이메일 제공에 동의한 사용자면") {
                it("프로필을 반환한다") {
                    stubResponse(KakaoOauthFixture.memberInfoResponse(isEmailVerified = true))

                    val profile = fetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN)

                    profile.provider shouldBe OauthProvider.KAKAO
                    profile.providerId shouldBe KakaoOauthFixture.PROVIDER_ID.toString()
                    profile.email shouldBe KakaoOauthFixture.EMAIL
                }
            }

            context("이메일 제공에 동의하지 않은 사용자면") {
                it("OAUTH_EMAIL_REQUIRED로 UnauthorizedException을 던진다") {
                    stubResponse(KakaoOauthFixture.memberInfoResponse(email = null, isEmailVerified = false))

                    val exception = shouldThrow<UnauthorizedException> { fetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_EMAIL_REQUIRED
                }
            }

            context("카카오가 4xx를 응답하면") {
                it("OauthTokenInvalidException을 던진다") {
                    stubFailure(HttpClientErrorException(HttpStatus.UNAUTHORIZED))

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }

            context("카카오가 5xx를 응답하면") {
                it("OauthProviderUnavailableException을 던진다") {
                    stubFailure(HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))

                    shouldThrow<OauthProviderUnavailableException> { fetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) }
                }
            }

            context("네트워크 오류로 카카오 호출이 실패하면") {
                it("OauthProviderUnavailableException을 던진다") {
                    stubFailure(ResourceAccessException("Connection timed out"))

                    shouldThrow<OauthProviderUnavailableException> { fetcher.fetchProfile(OauthFixture.OAUTH_ACCESS_TOKEN) }
                }
            }
        }
    })
