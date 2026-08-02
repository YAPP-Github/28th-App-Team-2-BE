package com.yapp.todakun.auth.adapter.oauth.apple

import com.yapp.todakun.auth.exception.OauthProviderUnavailableException
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.auth.fixture.AppleOauthFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

private const val AUTHORIZATION_CODE = "test-authorization-code"
private const val CLIENT_SECRET = "test-client-secret"

class AppleOauthTokenClientTest :
    DescribeSpec({
        val restClient = mockk<RestClient>()
        val requestBodyUriSpec = mockk<RestClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<RestClient.RequestBodySpec>()
        val responseSpec = mockk<RestClient.ResponseSpec>()
        val appleOauthClientSecretGenerator = mockk<AppleOauthClientSecretGenerator>()
        val properties = AppleOauthFixture.properties()
        val tokenClient = AppleOauthTokenClient(restClient, properties, appleOauthClientSecretGenerator)

        afterTest { clearMocks(restClient, requestBodyUriSpec, requestBodySpec, responseSpec, appleOauthClientSecretGenerator) }

        fun stubPost(endpoint: String): CapturingSlot<MultiValueMap<String, String>> {
            val bodySlot = slot<MultiValueMap<String, String>>()
            every { restClient.post() } returns requestBodyUriSpec
            every { requestBodyUriSpec.uri(endpoint) } returns requestBodySpec
            every { requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED) } returns requestBodySpec
            every { requestBodySpec.body(capture(bodySlot)) } returns requestBodySpec
            every { appleOauthClientSecretGenerator.generate(AppleOauthFixture.CLIENT_ID) } returns CLIENT_SECRET

            return bodySlot
        }

        describe("exchangeAuthorizationCode") {
            context("Apple 토큰 교환에 성공하면") {
                it("client_secret을 포함한 폼으로 요청하고 refresh_token을 반환한다") {
                    val bodySlot = stubPost(properties.tokenEndpoint)
                    every { requestBodySpec.retrieve() } returns responseSpec
                    every { responseSpec.hint(any(), any()) } returns responseSpec
                    every {
                        responseSpec.body(any<ParameterizedTypeReference<AppleOauthTokenResponse>>())
                    } returns AppleOauthTokenResponse(AppleOauthFixture.REFRESH_TOKEN)

                    val refreshToken = tokenClient.exchangeAuthorizationCode(AppleOauthFixture.CLIENT_ID, AUTHORIZATION_CODE)

                    refreshToken shouldBe AppleOauthFixture.REFRESH_TOKEN
                    bodySlot.captured["client_id"] shouldBe listOf(AppleOauthFixture.CLIENT_ID)
                    bodySlot.captured["client_secret"] shouldBe listOf(CLIENT_SECRET)
                    bodySlot.captured["grant_type"] shouldBe listOf("authorization_code")
                    bodySlot.captured["code"] shouldBe listOf(AUTHORIZATION_CODE)
                }
            }

            context("Apple 서버 호출이 실패하면") {
                it("OauthProviderUnavailableException을 던진다") {
                    stubPost(properties.tokenEndpoint)
                    every { requestBodySpec.retrieve() } throws ResourceAccessException("Connection timed out")

                    shouldThrow<OauthProviderUnavailableException> {
                        tokenClient.exchangeAuthorizationCode(AppleOauthFixture.CLIENT_ID, AUTHORIZATION_CODE)
                    }
                }
            }

            context("invalid_grant 등 Apple이 4xx로 거절하면") {
                it("OauthTokenInvalidException을 던진다") {
                    stubPost(properties.tokenEndpoint)
                    every { requestBodySpec.retrieve() } throws
                        HttpClientErrorException(HttpStatus.BAD_REQUEST, "invalid_grant")

                    shouldThrow<OauthTokenInvalidException> {
                        tokenClient.exchangeAuthorizationCode(AppleOauthFixture.CLIENT_ID, AUTHORIZATION_CODE)
                    }
                }
            }
        }

        describe("revoke") {
            context("Apple 계정 연결 해제에 성공하면") {
                it("client_secret을 포함한 폼으로 요청한다") {
                    val bodySlot = stubPost(properties.revokeEndpoint)
                    every { requestBodySpec.retrieve() } returns responseSpec
                    every { responseSpec.toBodilessEntity() } returns ResponseEntity.noContent().build()

                    tokenClient.revoke(AppleOauthFixture.CLIENT_ID, AppleOauthFixture.REFRESH_TOKEN)

                    bodySlot.captured["client_id"] shouldBe listOf(AppleOauthFixture.CLIENT_ID)
                    bodySlot.captured["client_secret"] shouldBe listOf(CLIENT_SECRET)
                    bodySlot.captured["token"] shouldBe listOf(AppleOauthFixture.REFRESH_TOKEN)
                    bodySlot.captured["token_type_hint"] shouldBe listOf("refresh_token")
                }
            }

            context("Apple 서버 호출이 실패하면") {
                it("OauthProviderUnavailableException을 던진다") {
                    stubPost(properties.revokeEndpoint)
                    every { requestBodySpec.retrieve() } throws ResourceAccessException("Connection timed out")

                    shouldThrow<OauthProviderUnavailableException> {
                        tokenClient.revoke(AppleOauthFixture.CLIENT_ID, AppleOauthFixture.REFRESH_TOKEN)
                    }
                }
            }
        }
    })
