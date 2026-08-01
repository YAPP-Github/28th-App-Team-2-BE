package com.yapp.todakun.auth.adapter.oauth.apple

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.exception.OauthEmailRequiredException
import com.yapp.todakun.auth.exception.OauthProviderUnavailableException
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.auth.fixture.AppleOauthFixture
import com.yapp.todakun.auth.port.outbound.AppleOauthCredentialPort
import com.yapp.todakun.shared.OauthProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

// appleIdTokenProcessor를 목킹하므로 실제로 파싱되지 않는다. every 스텁을 매칭시키는 임의의 토큰 문자열이면 충분하다.
private const val ID_TOKEN = "test-id-token"
private const val AUTHORIZATION_CODE = "test-authorization-code"

// AppleOauthProperties.issuer와 다른 issuer → 신뢰할 수 없는 발급자 경로를 재현한다.
private const val UNTRUSTED_ISSUER = "https://untrusted.example.com"

// AppleOauthProperties.clientIds에 등록되지 않은 audience → 미등록 클라이언트 경로를 재현한다.
private const val UNKNOWN_CLIENT_ID = "other-client-id"

class AppleOauthFetcherTest :
    DescribeSpec({
        val appleIdTokenProcessor = mockk<ConfigurableJWTProcessor<SecurityContext>>()
        val appleOauthTokenClient = mockk<AppleOauthTokenClient>()
        val appleOauthCredentialPort = mockk<AppleOauthCredentialPort>()
        val properties = AppleOauthFixture.properties()
        val fetcher = AppleOauthFetcher(appleIdTokenProcessor, properties, appleOauthTokenClient, appleOauthCredentialPort)

        afterTest { clearMocks(appleIdTokenProcessor, appleOauthTokenClient, appleOauthCredentialPort) }

        describe("fetchProfile") {
            context("authorizationCode가 없으면") {
                it("credential을 저장하지 않고 프로필을 반환한다") {
                    every { appleIdTokenProcessor.process(ID_TOKEN, null) } returns AppleOauthFixture.claims()

                    val profile = fetcher.fetchProfile(ID_TOKEN, authorizationCode = null)

                    profile.provider shouldBe OauthProvider.APPLE
                    profile.providerId shouldBe AppleOauthFixture.SUBJECT
                    profile.email shouldBe AppleOauthFixture.EMAIL
                    verify(exactly = 0) { appleOauthTokenClient.exchangeAuthorizationCode(any(), any()) }
                    verify(exactly = 0) { appleOauthCredentialPort.save(any(), any(), any()) }
                }
            }

            context("authorizationCode가 있으면") {
                it("authorization code를 교환해 credential을 저장하고 프로필을 반환한다") {
                    every { appleIdTokenProcessor.process(ID_TOKEN, null) } returns AppleOauthFixture.claims()
                    every {
                        appleOauthTokenClient.exchangeAuthorizationCode(AppleOauthFixture.CLIENT_ID, AUTHORIZATION_CODE)
                    } returns AppleOauthFixture.REFRESH_TOKEN
                    every {
                        appleOauthCredentialPort.save(
                            AppleOauthFixture.SUBJECT,
                            AppleOauthFixture.CLIENT_ID,
                            AppleOauthFixture.REFRESH_TOKEN,
                        )
                    } returns AppleOauthFixture.credential()

                    val profile = fetcher.fetchProfile(ID_TOKEN, AUTHORIZATION_CODE)

                    profile.providerId shouldBe AppleOauthFixture.SUBJECT
                    verify(exactly = 1) {
                        appleOauthCredentialPort.save(
                            AppleOauthFixture.SUBJECT,
                            AppleOauthFixture.CLIENT_ID,
                            AppleOauthFixture.REFRESH_TOKEN,
                        )
                    }
                }
            }

            context("Apple 서버 장애로 authorization code 교환이 실패해도") {
                it("credential 저장 실패를 삼키고 로그인은 성공한다") {
                    every { appleIdTokenProcessor.process(ID_TOKEN, null) } returns AppleOauthFixture.claims()
                    every {
                        appleOauthTokenClient.exchangeAuthorizationCode(AppleOauthFixture.CLIENT_ID, AUTHORIZATION_CODE)
                    } throws OauthProviderUnavailableException()

                    val profile = fetcher.fetchProfile(ID_TOKEN, AUTHORIZATION_CODE)

                    profile.providerId shouldBe AppleOauthFixture.SUBJECT
                    verify(exactly = 0) { appleOauthCredentialPort.save(any(), any(), any()) }
                }
            }

            context("credential 저장 중 예기치 못한 예외가 발생하면") {
                it("삼키지 않고 전파해 로그인도 실패한다") {
                    every { appleIdTokenProcessor.process(ID_TOKEN, null) } returns AppleOauthFixture.claims()
                    every {
                        appleOauthTokenClient.exchangeAuthorizationCode(AppleOauthFixture.CLIENT_ID, AUTHORIZATION_CODE)
                    } returns AppleOauthFixture.REFRESH_TOKEN
                    every {
                        appleOauthCredentialPort.save(
                            AppleOauthFixture.SUBJECT,
                            AppleOauthFixture.CLIENT_ID,
                            AppleOauthFixture.REFRESH_TOKEN,
                        )
                    } throws IllegalStateException("unexpected failure")

                    shouldThrow<IllegalStateException> { fetcher.fetchProfile(ID_TOKEN, AUTHORIZATION_CODE) }
                }
            }

            context("audience에 등록된 clientId가 없으면") {
                it("OauthTokenInvalidException을 던진다") {
                    every {
                        appleIdTokenProcessor.process(ID_TOKEN, null)
                    } returns AppleOauthFixture.claims(audience = UNKNOWN_CLIENT_ID)

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(ID_TOKEN, null) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }

            context("issuer가 신뢰할 수 없는 값이면") {
                it("OauthTokenInvalidException을 던진다") {
                    every {
                        appleIdTokenProcessor.process(ID_TOKEN, null)
                    } returns AppleOauthFixture.claims(issuer = UNTRUSTED_ISSUER)

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(ID_TOKEN, null) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }

            context("이메일이 인증되지 않았으면") {
                it("OauthEmailRequiredException을 던진다") {
                    every {
                        appleIdTokenProcessor.process(ID_TOKEN, null)
                    } returns AppleOauthFixture.claims(emailVerified = false)

                    val exception = shouldThrow<OauthEmailRequiredException> { fetcher.fetchProfile(ID_TOKEN, null) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_EMAIL_REQUIRED
                }
            }
        }
    })
