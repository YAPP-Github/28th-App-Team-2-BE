package com.yapp.todakun.auth.adapter.jwt.access

import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.common.exception.UnauthorizedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

// HMAC-SHA 서명에 필요한 최소 키 길이(32바이트)를 만족해야 한다. 짧으면 WeakKeyException이 발생한다.
private const val TEST_SECRET = "3b1c2e7f9a4d6081c5e2f7a9b3d6180425e8f1a7c4b9d2e6f3081a5c7e9b2d4"

// 서명 위조를 재현하기 위한, TEST_SECRET과 다른 별도의 시크릿(길이 제약은 동일).
private const val OTHER_SECRET = "9f2d5b8e1a4c7063d9f6b2a5e8c1043769f2c5b8e1d4a7063f9c2b5e8a1d4f7"

// JWT 구조(점 3개로 구분된 세그먼트)조차 아닌 문자열 → MalformedJwtException 경로를 재현한다.
private const val MALFORMED_TOKEN = "invalid-token"

@ExperimentalUuidApi
class AccessTokenAdapterTest :
    DescribeSpec(
        {
            val properties = AccessTokenProperties(secret = TEST_SECRET, expirySeconds = 3600L)
            val accessTokenAdapter = AccessTokenAdapter(properties)
            val memberId = Uuid.generateV7().toJavaUuid()

            describe("generate") {
                context("memberId가 주어지면") {
                    it("서명된 토큰, jti, 만료 시간을 발급한다") {
                        val issued = accessTokenAdapter.generate(memberId)

                        issued.value.shouldNotBeBlank()
                        issued.jti.shouldNotBeBlank()
                        issued.expiresInSeconds shouldBe properties.expirySeconds
                    }
                }
            }

            describe("parse") {
                context("generate로 발급한 토큰이면") {
                    it("memberId와 jti를 복원한다") {
                        val issued = accessTokenAdapter.generate(memberId)

                        val claims = accessTokenAdapter.parse(issued.value)

                        claims.memberId shouldBe memberId
                        claims.jti shouldBe issued.jti
                        claims.remainingSeconds shouldBeLessThanOrEqual properties.expirySeconds
                    }
                }

                context("만료된 토큰이면") {
                    it("ACCESS_TOKEN_EXPIRED로 UnauthorizedException을 던진다") {
                        val expiredAdapter = AccessTokenAdapter(properties.copy(expirySeconds = -10L))
                        val expiredToken = expiredAdapter.generate(memberId).value

                        val exception = shouldThrow<UnauthorizedException> { accessTokenAdapter.parse(expiredToken) }

                        exception.errorCode shouldBe AuthErrorCode.ACCESS_TOKEN_EXPIRED
                    }
                }

                context("JWT 형식이 아닌 토큰이면") {
                    it("ACCESS_TOKEN_INVALID로 UnauthorizedException을 던진다") {
                        val exception = shouldThrow<UnauthorizedException> { accessTokenAdapter.parse(MALFORMED_TOKEN) }

                        exception.errorCode shouldBe AuthErrorCode.ACCESS_TOKEN_INVALID
                    }
                }

                context("서명이 위조된 토큰이면") {
                    it("ACCESS_TOKEN_INVALID로 UnauthorizedException을 던진다") {
                        val otherAdapter = AccessTokenAdapter(properties.copy(secret = OTHER_SECRET))
                        val tamperedToken = otherAdapter.generate(memberId).value

                        val exception = shouldThrow<UnauthorizedException> { accessTokenAdapter.parse(tamperedToken) }

                        exception.errorCode shouldBe AuthErrorCode.ACCESS_TOKEN_INVALID
                    }
                }
            }
        },
    )
