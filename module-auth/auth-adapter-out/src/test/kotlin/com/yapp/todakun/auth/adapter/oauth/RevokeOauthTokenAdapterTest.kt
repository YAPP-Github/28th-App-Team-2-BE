package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.AppleOauthCredential
import com.yapp.todakun.auth.adapter.oauth.apple.AppleOauthTokenClient
import com.yapp.todakun.auth.exception.OauthProviderUnavailableException
import com.yapp.todakun.auth.port.outbound.AppleOauthCredentialPort
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
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

        describe("revokeIfApplicable") {
            context("provider가 KAKAO/GOOGLE이면") {
                it("아무 동작도 하지 않는다") {
                    adapter.revokeIfApplicable(OauthProvider.KAKAO, PROVIDER_ID)
                    adapter.revokeIfApplicable(OauthProvider.GOOGLE, PROVIDER_ID)

                    verify(exactly = 0) { appleOauthCredentialPort.find(any()) }
                    verify(exactly = 0) { appleOauthTokenClient.revoke(any(), any()) }
                }
            }

            context("provider가 APPLE이지만 저장된 credential이 없으면") {
                it("revoke/delete를 호출하지 않는다") {
                    every { appleOauthCredentialPort.find(PROVIDER_ID) } returns null

                    adapter.revokeIfApplicable(OauthProvider.APPLE, PROVIDER_ID)

                    verify(exactly = 0) { appleOauthTokenClient.revoke(any(), any()) }
                    verify(exactly = 0) { appleOauthCredentialPort.delete(any()) }
                }
            }

            context("provider가 APPLE이고 저장된 credential이 있으면") {
                it("Apple 계정 연결을 해제하고 credential을 삭제한다") {
                    val credential = AppleOauthCredential.reconstitute(CREDENTIAL_ID, PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)
                    every { appleOauthCredentialPort.find(PROVIDER_ID) } returns credential
                    every { appleOauthTokenClient.revoke(CLIENT_ID, REFRESH_TOKEN) } just Runs
                    every { appleOauthCredentialPort.delete(PROVIDER_ID) } just Runs

                    adapter.revokeIfApplicable(OauthProvider.APPLE, PROVIDER_ID)

                    verify(exactly = 1) { appleOauthTokenClient.revoke(CLIENT_ID, REFRESH_TOKEN) }
                    verify(exactly = 1) { appleOauthCredentialPort.delete(PROVIDER_ID) }
                }
            }

            context("revoke 호출이 실패하면") {
                it("로그만 남기고 credential은 그대로 삭제한다") {
                    val credential = AppleOauthCredential.reconstitute(CREDENTIAL_ID, PROVIDER_ID, CLIENT_ID, REFRESH_TOKEN)
                    every { appleOauthCredentialPort.find(PROVIDER_ID) } returns credential
                    every { appleOauthTokenClient.revoke(CLIENT_ID, REFRESH_TOKEN) } throws OauthProviderUnavailableException()
                    every { appleOauthCredentialPort.delete(PROVIDER_ID) } just Runs

                    adapter.revokeIfApplicable(OauthProvider.APPLE, PROVIDER_ID)

                    verify(exactly = 1) { appleOauthCredentialPort.delete(PROVIDER_ID) }
                }
            }
        }
    })
