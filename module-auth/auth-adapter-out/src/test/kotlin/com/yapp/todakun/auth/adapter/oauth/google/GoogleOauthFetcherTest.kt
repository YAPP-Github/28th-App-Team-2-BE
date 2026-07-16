package com.yapp.todakun.auth.adapter.oauth.google

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import com.yapp.todakun.auth.fixture.GoogleOauthFixture
import com.yapp.todakun.common.exception.UnauthorizedException
import com.yapp.todakun.shared.OauthProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.text.ParseException

// googleIdTokenProcessor를 목킹하므로 실제로 파싱되지 않는다. every 스텁을 매칭시키는 임의의 토큰 문자열이면 충분하다.
private const val ID_TOKEN = "test-id-token"

// GoogleOauthProperties.issuers에 등록되지 않은 issuer → 신뢰할 수 없는 발급자 경로를 재현한다.
private const val UNTRUSTED_ISSUER = "https://untrusted.example.com"

// GoogleOauthProperties.clientIds에 등록되지 않은 audience → 미등록 클라이언트 경로를 재현한다.
private const val UNKNOWN_CLIENT_ID = "other-client-id"

class GoogleOauthFetcherTest :
    DescribeSpec({
        val googleIdTokenProcessor = mockk<ConfigurableJWTProcessor<SecurityContext>>()
        val properties = GoogleOauthFixture.properties()
        val fetcher = GoogleOauthFetcher(googleIdTokenProcessor, properties)

        afterTest { clearMocks(googleIdTokenProcessor) }

        describe("fetchProfile") {
            context("email_verified가 Boolean 타입 true면") {
                it("프로필을 반환한다") {
                    every { googleIdTokenProcessor.process(ID_TOKEN, null) } returns GoogleOauthFixture.claims(emailVerified = true)

                    val profile = fetcher.fetchProfile(ID_TOKEN)

                    profile.provider shouldBe OauthProvider.GOOGLE
                    profile.providerId shouldBe GoogleOauthFixture.SUBJECT
                    profile.email shouldBe GoogleOauthFixture.EMAIL
                }
            }

            context("email_verified가 문자열 true면") {
                it("프로필을 반환한다") {
                    every { googleIdTokenProcessor.process(ID_TOKEN, null) } returns GoogleOauthFixture.claims(emailVerified = "true")

                    val profile = fetcher.fetchProfile(ID_TOKEN)

                    profile.email shouldBe GoogleOauthFixture.EMAIL
                }
            }

            context("email_verified가 false면") {
                it("OAUTH_EMAIL_REQUIRED로 UnauthorizedException을 던진다") {
                    every { googleIdTokenProcessor.process(ID_TOKEN, null) } returns GoogleOauthFixture.claims(emailVerified = false)

                    val exception = shouldThrow<UnauthorizedException> { fetcher.fetchProfile(ID_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_EMAIL_REQUIRED
                }
            }

            context("id 토큰 파싱에 실패하면") {
                it("OauthTokenInvalidException을 던진다") {
                    every { googleIdTokenProcessor.process(ID_TOKEN, null) } throws ParseException("parse error", 0)

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(ID_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }

            context("서명 검증에 실패하면") {
                it("OauthTokenInvalidException을 던진다") {
                    every { googleIdTokenProcessor.process(ID_TOKEN, null) } throws BadJOSEException("signature error")

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(ID_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }

            context("JOSE 처리에 실패하면") {
                it("OauthTokenInvalidException을 던진다") {
                    every { googleIdTokenProcessor.process(ID_TOKEN, null) } throws JOSEException("jose error")

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(ID_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }

            context("issuer가 신뢰할 수 없는 값이면") {
                it("OauthTokenInvalidException을 던진다") {
                    every {
                        googleIdTokenProcessor.process(ID_TOKEN, null)
                    } returns GoogleOauthFixture.claims(issuer = UNTRUSTED_ISSUER)

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(ID_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }

            context("audience에 등록된 clientId가 없으면") {
                it("OauthTokenInvalidException을 던진다") {
                    every {
                        googleIdTokenProcessor.process(ID_TOKEN, null)
                    } returns GoogleOauthFixture.claims(audience = UNKNOWN_CLIENT_ID)

                    val exception = shouldThrow<OauthTokenInvalidException> { fetcher.fetchProfile(ID_TOKEN) }

                    exception.errorCode shouldBe AuthErrorCode.OAUTH_TOKEN_INVALID
                }
            }
        }
    })
