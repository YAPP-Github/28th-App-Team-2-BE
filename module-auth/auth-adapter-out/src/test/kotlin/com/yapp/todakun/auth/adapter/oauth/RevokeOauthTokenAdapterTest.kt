package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.AppleOauthCredential
import com.yapp.todakun.auth.adapter.oauth.apple.AppleOauthTokenClient
import com.yapp.todakun.auth.exception.OauthProviderUnavailableException
import com.yapp.todakun.auth.port.outbound.AppleOauthCredentialPort
import com.yapp.todakun.shared.OauthProvider
import com.yapp.todakun.shared.OauthRevokeCredential
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

private val CREDENTIAL_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private const val PROVIDER_ID = "apple-provider-id"
private const val CLIENT_ID = "com.yapp.todakun"
private const val REFRESH_TOKEN = "apple-refresh-token"

class RevokeOauthTokenAdapterTest :
    DescribeSpec({
        val appleOauthCredentialPort = mockk<AppleOauthCredentialPort>()
        val appleOauthTokenClient = mockk<AppleOauthTokenClient>()
        val adapter = RevokeOauthTokenAdapter(appleOauthCredentialPort, appleOauthTokenClient)

        afterTest { clearMocks(appleOauthCredentialPort, appleOauthTokenClient) }

        describe("prepareRevoke") {
            context("provider가 KAKAO/GOOGLE이면") {
                it("아무 동작도 하지 않고 null을 반환한다") {
                    adapter.prepareRevoke(OauthProvider.KAKAO, PROVIDER_ID) shouldBe null
                    adapter.prepareRevoke(OauthProvider.GOOGLE, PROVIDER_ID) shouldBe null

                    verify(exactly = 0) { appleOauthCredentialPort.find(any()) }
                }
            }

            context("provider가 APPLE이지만 저장된 credential이 없으면") {
                it("delete를 호출하지 않고 null을 반환한다") {
                    every { appleOauthCredentialPort.find(PROVIDER_ID) } returns null

                    adapter.prepareRevoke(OauthProvider.APPLE, PROVIDER_ID) shouldBe null

                    verify(exactly = 0) { appleOauthCredentialPort.delete(any()) }
                }
            }

            context("provider가 APPLE이고 저장된 credential이 있으면") {
                it("credential을 삭제하고 revoke에 필요한 정보를 반환한다") {
                    val credential = AppleOauthCredential.reconstitute(CREDENTIAL_ID, PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)
                    every { appleOauthCredentialPort.find(PROVIDER_ID) } returns credential
                    every { appleOauthCredentialPort.delete(PROVIDER_ID) } just Runs

                    val result = adapter.prepareRevoke(OauthProvider.APPLE, PROVIDER_ID)

                    result shouldBe OauthRevokeCredential(PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)
                    verify(exactly = 1) { appleOauthCredentialPort.delete(PROVIDER_ID) }
                }
            }
        }

        describe("revoke") {
            val credential = OauthRevokeCredential(PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)

            context("revoke 호출이 성공하면") {
                it("Apple 계정 연결을 해제한다") {
                    every { appleOauthTokenClient.revoke(CLIENT_ID, REFRESH_TOKEN) } just Runs

                    adapter.revoke(credential)

                    verify(exactly = 1) { appleOauthTokenClient.revoke(CLIENT_ID, REFRESH_TOKEN) }
                }
            }

            context("revoke 호출이 실패하면") {
                it("예외를 전파하지 않고 로그만 남긴다") {
                    every { appleOauthTokenClient.revoke(CLIENT_ID, REFRESH_TOKEN) } throws OauthProviderUnavailableException()

                    adapter.revoke(credential)

                    verify(exactly = 1) { appleOauthTokenClient.revoke(CLIENT_ID, REFRESH_TOKEN) }
                }
            }
        }
    })
